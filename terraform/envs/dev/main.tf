terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket       = "d14-terraform-state"
    key          = "dev/terraform.tfstate"
    region       = "ap-northeast-2"
    profile      = "d14"
    use_lockfile = true
    encrypt      = true
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

module "app" {
  source = "../../modules/app"

  aws_region              = var.aws_region
  aws_profile             = var.aws_profile
  project_name            = var.project_name
  environment             = var.environment
  ec2_key_pair_name       = var.ec2_key_pair_name
  allowed_ssh_cidr        = var.allowed_ssh_cidr
  instance_type           = var.instance_type
  ami_id                  = var.ami_id
  spot_max_price          = var.spot_max_price
  root_volume_size        = var.root_volume_size
  s3_bucket_suffix        = var.s3_bucket_suffix
  s3_cors_allowed_origins = var.s3_cors_allowed_origins
  log_retention_days      = var.log_retention_days
  discord_webhook_url     = var.discord_webhook_url
}
