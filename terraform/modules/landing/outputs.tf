output "bucket_name" {
  description = "정적 산출물 sync 대상 S3 버킷"
  value       = aws_s3_bucket.this.id
}

output "cloudfront_distribution_id" {
  description = "배포 후 캐시 무효화(invalidation) 에 사용"
  value       = aws_cloudfront_distribution.this.id
}

output "cloudfront_domain_name" {
  description = "가비아에 www CNAME 으로 등록할 CloudFront 도메인"
  value       = aws_cloudfront_distribution.this.domain_name
}

output "deploy_role_arn" {
  description = "GitHub Actions 가 assume 할 배포 역할 ARN (레포 Secret AWS_DEPLOY_ROLE_ARN 에 등록)"
  value       = aws_iam_role.deploy.arn
}

# 가비아에 수동 등록할 ACM 검증용 CNAME (name -> value)
output "acm_validation_records" {
  description = "ACM DNS 검증을 위해 가비아에 추가할 CNAME 레코드"
  value = [
    for o in aws_acm_certificate.this.domain_validation_options : {
      name  = o.resource_record_name
      type  = o.resource_record_type
      value = o.resource_record_value
    }
  ]
}
