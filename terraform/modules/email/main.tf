terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    archive = {
      source = "hashicorp/archive"
    }
  }
}

# 이 모듈의 모든 리소스는 SES 수신(inbound)이 지원되는 리전에서 생성되어야 한다.
# 서울(ap-northeast-2)은 SES 수신 미지원이라, 호출측에서 us-east-1 provider 를 주입한다.

locals {
  name = "${var.project_name}-${var.environment}-mail"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# ---------------------------------------------------------------------------
# SES — 도메인 소유 검증 + DKIM
#   검증 TXT / DKIM CNAME 은 DNS(가비아)에 수동 등록해야 한다. outputs 참고.
# ---------------------------------------------------------------------------
resource "aws_ses_domain_identity" "this" {
  domain = var.mail_domain
}

resource "aws_ses_domain_dkim" "this" {
  domain = aws_ses_domain_identity.this.domain
}

# 포워딩 대상 주소 — SES 샌드박스에서도 이 주소로 전달할 수 있도록 verify.
# (콘솔/메일로 확인 링크를 눌러야 최종 verified 된다.)
resource "aws_ses_email_identity" "forward_to" {
  email = var.forward_to
}

# ---------------------------------------------------------------------------
# S3 — 수신 원문(MIME) 임시 저장소
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "inbox" {
  bucket = "${local.name}-inbox-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "inbox" {
  bucket                  = aws_s3_bucket.inbox.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "inbox" {
  bucket = aws_s3_bucket.inbox.id

  rule {
    id     = "expire-raw-mail"
    status = "Enabled"

    filter {
      prefix = "inbox/"
    }

    expiration {
      days = var.retention_days
    }
  }
}

# SES 가 이 버킷에 수신 원문을 쓸 수 있도록 허용
data "aws_iam_policy_document" "inbox" {
  statement {
    sid       = "AllowSESPuts"
    effect    = "Allow"
    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.inbox.arn}/inbox/*"]

    principals {
      type        = "Service"
      identifiers = ["ses.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:Referer"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_s3_bucket_policy" "inbox" {
  bucket = aws_s3_bucket.inbox.id
  policy = data.aws_iam_policy_document.inbox.json
}

# ---------------------------------------------------------------------------
# Lambda — S3 의 원문을 읽어 forward_to 로 재전송 (From rewrite + Reply-To 보존)
# ---------------------------------------------------------------------------
data "archive_file" "forwarder" {
  type        = "zip"
  source_file = "${path.module}/lambda/forwarder.mjs"
  output_path = "${path.module}/lambda/forwarder.zip"
}

resource "aws_iam_role" "forwarder" {
  name = "${local.name}-forwarder-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "forwarder_logs" {
  role       = aws_iam_role.forwarder.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

data "aws_iam_policy_document" "forwarder" {
  statement {
    sid       = "ReadRawMail"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.inbox.arn}/inbox/*"]
  }

  statement {
    sid       = "SendForwarded"
    effect    = "Allow"
    actions   = ["ses:SendRawEmail"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "forwarder" {
  name   = "${local.name}-forwarder-policy"
  role   = aws_iam_role.forwarder.id
  policy = data.aws_iam_policy_document.forwarder.json
}

resource "aws_lambda_function" "forwarder" {
  function_name    = "${local.name}-forwarder"
  role             = aws_iam_role.forwarder.arn
  handler          = "forwarder.handler"
  runtime          = "nodejs20.x"
  timeout          = 30
  memory_size      = 128
  filename         = data.archive_file.forwarder.output_path
  source_code_hash = data.archive_file.forwarder.output_base64sha256

  environment {
    variables = {
      MAIL_BUCKET  = aws_s3_bucket.inbox.id
      MAIL_PREFIX  = "inbox/"
      FORWARD_TO   = var.forward_to
      FORWARD_FROM = var.recipients[0]
    }
  }

  tags = { Name = "${local.name}-forwarder" }
}

resource "aws_lambda_permission" "allow_ses" {
  statement_id   = "AllowExecutionFromSES"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.forwarder.function_name
  principal      = "ses.amazonaws.com"
  source_account = data.aws_caller_identity.current.account_id
}

# ---------------------------------------------------------------------------
# SES 수신 규칙 — team@hilit.my 로 온 메일: S3 저장 후 Lambda 호출
# ---------------------------------------------------------------------------
resource "aws_ses_receipt_rule_set" "this" {
  rule_set_name = "${local.name}-rules"
}

resource "aws_ses_active_receipt_rule_set" "this" {
  rule_set_name = aws_ses_receipt_rule_set.this.rule_set_name
}

resource "aws_ses_receipt_rule" "forward" {
  name          = "${local.name}-forward"
  rule_set_name = aws_ses_receipt_rule_set.this.rule_set_name
  recipients    = var.recipients
  enabled       = true
  scan_enabled  = true

  s3_action {
    position          = 1
    bucket_name       = aws_s3_bucket.inbox.id
    object_key_prefix = "inbox/"
  }

  lambda_action {
    position        = 2
    function_arn    = aws_lambda_function.forwarder.arn
    invocation_type = "Event"
  }

  depends_on = [
    aws_s3_bucket_policy.inbox,
    aws_lambda_permission.allow_ses,
  ]
}
