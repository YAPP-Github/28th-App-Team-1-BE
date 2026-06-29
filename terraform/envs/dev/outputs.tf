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
