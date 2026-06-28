variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  description = "사용할 AWS CLI named profile (~/.aws/credentials 의 섹션 이름)"
  type        = string
  default     = "d14"
}

variable "project_name" {
  description = "프로젝트 이름 (리소스 네이밍에 사용)"
  type        = string
  default     = "d14"
}

variable "environment" {
  description = "환경 태그"
  type        = string
  default     = "prod"
}

variable "ec2_key_pair_name" {
  description = "SSH 접속용 기존 AWS Key Pair 이름"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "SSH 접속 허용 CIDR (운영시 팀 IP/32로 좁힐 것)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "spot_max_price" {
  description = "Spot 가격 상한 (USD/hr). 시세가 이를 초과하면 인스턴스가 stop 된다."
  type        = string
  default     = "0.008"
}

variable "root_volume_size" {
  description = "루트 EBS 볼륨 크기 (GB)"
  type        = number
  default     = 20
}

variable "s3_bucket_suffix" {
  description = "S3 버킷 이름 suffix (전 세계 고유해야 하므로 랜덤 문자열 권장)"
  type        = string
  default     = "media"
}

variable "s3_cors_allowed_origins" {
  description = "S3 CORS 허용 origin 목록 (브라우저에서 직접 업로드/다운로드 시)"
  type        = list(string)
  default     = ["*"]
}
