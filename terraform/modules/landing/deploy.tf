# ---------------------------------------------------------------------------
# GitHub Actions OIDC 배포 역할
#   landing-page 레포의 main 브랜치 워크플로우가 장기 키 없이 AWS 역할을 assume 해
#   S3 sync + CloudFront invalidation 만 수행하도록 최소 권한으로 부여한다.
# ---------------------------------------------------------------------------

# GitHub Actions OIDC provider (계정당 1개)
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]
}

data "aws_iam_policy_document" "deploy_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # 지정한 레포의 main 브랜치 워크플로우만 허용.
    # Team-Hilit 조직이 OIDC sub 에 조직/레포 숫자 ID(@<id>)를 포함하도록
    # 커스터마이즈했으므로(예: repo:Team-Hilit@320132304/landing-page@1345579467:...),
    # StringLike + @* 로 ID 부분을 와일드카드 처리한다.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${split("/", var.github_repo)[0]}@*/${split("/", var.github_repo)[1]}@*:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "deploy" {
  name               = "${local.name}-deploy"
  assume_role_policy = data.aws_iam_policy_document.deploy_assume.json
}

data "aws_iam_policy_document" "deploy" {
  # 정적 산출물 동기화
  statement {
    sid       = "SyncBucket"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.this.arn]
  }

  statement {
    sid       = "WriteObjects"
    effect    = "Allow"
    actions   = ["s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.this.arn}/*"]
  }

  # 배포 후 캐시 무효화
  statement {
    sid       = "Invalidate"
    effect    = "Allow"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = [aws_cloudfront_distribution.this.arn]
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "${local.name}-deploy"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}
