variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "aws_profile" {
  type    = string
  default = "d14"
}

variable "project_name" {
  type    = string
  default = "d14"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "ec2_key_pair_name" {
  type = string
}

variable "allowed_ssh_cidr" {
  type    = string
  default = "0.0.0.0/0"
}

variable "instance_type" {
  type    = string
  default = "t3.micro"
}

variable "ami_id" {
  type    = string
  default = "ami-0afe1fd15675c3f15"
}

variable "spot_max_price" {
  type    = string
  default = "0.008"
}

variable "root_volume_size" {
  type    = number
  default = 20
}

variable "s3_bucket_suffix" {
  type    = string
  default = "storage"
}

variable "s3_cors_allowed_origins" {
  type    = list(string)
  default = ["*"]
}

variable "log_retention_days" {
  type    = number
  default = 14
}

variable "discord_webhook_url" {
  type      = string
  sensitive = true
}

variable "cpu_credit_low_threshold" {
  type    = number
  default = 30
}

variable "disk_fstype" {
  type    = string
  default = "ext4"
}

# --- 랜딩페이지(정적 사이트) 호스팅 ---
variable "landing_domain_name" {
  type    = string
  default = "www.hilit.my"
}

variable "landing_bucket_name" {
  type    = string
  default = "d14-hilit-landing"
}

# --- 팀 수신 메일(SES inbound → Lambda 포워딩) ---
variable "mail_domain" {
  type    = string
  default = "hilit.my"
}

variable "mail_recipients" {
  type    = list(string)
  default = ["team@hilit.my"]
}

# 포워딩 대상 주소 (기존 팀/개인 Gmail 등). tfvars 에서 채운다.
variable "mail_forward_to" {
  type = string
}
