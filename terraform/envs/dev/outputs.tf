output "elastic_ip" {
  value = module.app.elastic_ip
}

output "spot_instance_id" {
  value = module.app.spot_instance_id
}

output "ecr_repository_url" {
  value = module.app.ecr_repository_url
}

output "ecr_repository_name" {
  value = module.app.ecr_repository_name
}

output "s3_bucket_name" {
  value = module.app.s3_bucket_name
}

output "s3_bucket_arn" {
  value = module.app.s3_bucket_arn
}

output "vpc_id" {
  value = module.app.vpc_id
}

output "ssh_command" {
  value = module.app.ssh_command
}

output "cloudwatch_log_group_name" {
  value = module.app.cloudwatch_log_group_name
}

# --- 랜딩페이지 ---
output "landing_bucket_name" {
  value = module.landing.bucket_name
}

output "landing_cloudfront_distribution_id" {
  value = module.landing.cloudfront_distribution_id
}

output "landing_cloudfront_domain_name" {
  value = module.landing.cloudfront_domain_name
}

output "landing_acm_validation_records" {
  value = module.landing.acm_validation_records
}

output "landing_deploy_role_arn" {
  value = module.landing.deploy_role_arn
}
