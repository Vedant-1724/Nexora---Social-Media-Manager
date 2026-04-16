output "eks_cluster_endpoint" {
  description = "EKS API server URL"
  value       = module.platform.eks_cluster_endpoint
}

output "rds_endpoint" {
  description = "PostgreSQL connection endpoint"
  value       = module.platform.rds_endpoint
}

output "redis_endpoint" {
  description = "Redis cache endpoint"
  value       = module.platform.redis_endpoint
}

output "ecr_repository_urls" {
  description = "ECR repository URLs per service"
  value       = module.platform.ecr_repository_urls
}

output "s3_media_bucket" {
  description = "S3 media storage bucket name"
  value       = module.platform.s3_media_bucket
}
