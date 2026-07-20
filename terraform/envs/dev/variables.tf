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
