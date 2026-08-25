terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      # CloudFront 용 ACM 인증서는 us-east-1 에 있어야 하므로 provider alias 를 주입받는다.
      configuration_aliases = [aws.us_east_1]
    }
  }
}

locals {
  name = "${var.project_name}-${var.environment}-landing"
}

# ---------------------------------------------------------------------------
# S3 — 정적 산출물 저장소 (비공개, CloudFront OAC 로만 접근)
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "this" {
  bucket = var.bucket_name
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket                  = aws_s3_bucket.this.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront 가 S3 에 서명된 요청으로 접근하기 위한 OAC
resource "aws_cloudfront_origin_access_control" "this" {
  name                              = local.name
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# ---------------------------------------------------------------------------
# ACM — www 도메인 TLS 인증서 (반드시 us-east-1)
#   DNS 가 가비아라 검증 CNAME 은 수동 등록한다.
#   validation 리소스는 레코드가 등록되어 ISSUED 될 때까지 대기한다.
# ---------------------------------------------------------------------------
resource "aws_acm_certificate" "this" {
  provider          = aws.us_east_1
  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "this" {
  provider        = aws.us_east_1
  certificate_arn = aws_acm_certificate.this.arn

  timeouts {
    create = "75m"
  }
}

# ---------------------------------------------------------------------------
# CloudFront — HTTPS 배포
# ---------------------------------------------------------------------------
resource "aws_cloudfront_distribution" "this" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = [var.domain_name]
  price_class         = var.price_class
  comment             = local.name

  origin {
    domain_name              = aws_s3_bucket.this.bucket_regional_domain_name
    origin_id                = "s3-${aws_s3_bucket.this.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.this.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-${aws_s3_bucket.this.id}"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    # AWS 관리형 CachingOptimized 정책
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  # 정적 단일 페이지 — 없는 경로는 index.html 로 폴백(향후 라우팅 대비)
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.this.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

# ---------------------------------------------------------------------------
# S3 버킷 정책 — 이 CloudFront 배포(OAC)만 오브젝트 읽기 허용
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "bucket" {
  statement {
    sid       = "AllowCloudFrontOAC"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.this.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.this.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.bucket.json
}
