variable "aws_region" {
  description = "AWS region for the dev environment"
  type        = string
  default     = "ap-south-1"
}

variable "db_password" {
  description = "PostgreSQL master password for dev"
  type        = string
  sensitive   = true
}
