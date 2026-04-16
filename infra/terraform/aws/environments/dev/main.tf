terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.95"
    }
  }

  # Uncomment for remote state in production
  # backend "s3" {
  #   bucket         = "nexora-terraform-state"
  #   key            = "dev/terraform.tfstate"
  #   region         = "ap-south-1"
  #   dynamodb_table = "nexora-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "nexora"
      Environment = "dev"
      ManagedBy   = "terraform"
    }
  }
}

module "platform" {
  source = "../../modules/platform"

  name_prefix  = "nexora-dev"
  aws_region   = var.aws_region
  environment  = "dev"
  db_password  = var.db_password

  vpc_cidr           = "10.0.0.0/16"
  availability_zones = ["${var.aws_region}a", "${var.aws_region}b"]

  db_instance_class      = "db.t4g.medium"
  redis_node_type        = "cache.t4g.micro"
  eks_node_instance_type = "t3.medium"
  eks_desired_capacity   = 2
  eks_max_capacity       = 4
  eks_min_capacity       = 1
}
