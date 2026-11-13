output "ecr_repository_url" { value = aws_ecr_repository.app.repository_url }
output "service_url" { value = "http://${aws_lb.app.dns_name}" }
output "ecs_cluster" { value = aws_ecs_cluster.main.name }
output "database_secret_arn" {
  value     = aws_db_instance.billing.master_user_secret[0].secret_arn
  sensitive = true
}
output "alarm_topic_arn" { value = aws_sns_topic.alarms.arn }
