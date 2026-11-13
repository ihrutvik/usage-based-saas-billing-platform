variable "aws_region" {
  type    = string
  default = "ap-south-1"
}
variable "environment" {
  type    = string
  default = "production"
}
variable "service_name" {
  type    = string
  default = "usage-billing"
}
variable "image_tag" {
  type    = string
  default = "latest"
}
variable "vpc_cidr" {
  type    = string
  default = "10.40.0.0/16"
}
variable "desired_count" {
  type    = number
  default = 2
}
variable "task_cpu" {
  type    = number
  default = 1024
}
variable "task_memory" {
  type    = number
  default = 2048
}
variable "db_instance_class" {
  type    = string
  default = "db.t4g.medium"
}
variable "alarm_email" {
  type    = string
  default = ""
}
