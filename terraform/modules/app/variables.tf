variable "aws_region" {
  description = "AWS 리전"
  type        = string
}

variable "aws_profile" {
  description = "AWS CLI named profile"
  type        = string
}

variable "project_name" {
  description = "프로젝트 이름 (리소스 네이밍에 사용)"
  type        = string
}

variable "environment" {
  description = "환경 태그 (dev / prod)"
  type        = string
}

variable "ec2_key_pair_name" {
  description = "SSH 접속용 AWS Key Pair 이름"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "SSH 접속 허용 CIDR"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "spot_max_price" {
  description = "Spot 가격 상한 (USD/hr)"
  type        = string
  default     = "0.008"
}

variable "root_volume_size" {
  description = "루트 EBS 볼륨 크기 (GB)"
  type        = number
  default     = 20
}

variable "s3_bucket_suffix" {
  description = "S3 버킷 이름 suffix"
  type        = string
  default     = "media"
}

variable "s3_cors_allowed_origins" {
  description = "S3 CORS 허용 origin 목록"
  type        = list(string)
  default     = ["*"]
}
