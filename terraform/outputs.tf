output "elastic_ip" {
  description = "EC2 Elastic IP (퍼블릭)"
  value       = aws_eip.app.public_ip
}

output "spot_instance_id" {
  description = "Spot Instance ID"
  value       = aws_spot_instance_request.app.spot_instance_id
}

output "ecr_repository_url" {
  description = "ECR 레포지토리 URL"
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_repository_name" {
  description = "ECR 레포지토리 이름"
  value       = aws_ecr_repository.app.name
}

output "s3_bucket_name" {
  description = "미디어 저장용 S3 버킷 이름"
  value       = aws_s3_bucket.media.bucket
}

output "s3_bucket_arn" {
  description = "S3 버킷 ARN"
  value       = aws_s3_bucket.media.arn
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "ssh_command" {
  description = "EC2 SSH 접속 명령어 예시"
  value       = "ssh -i ~/.ssh/${var.ec2_key_pair_name}.pem ubuntu@${aws_eip.app.public_ip}"
}
