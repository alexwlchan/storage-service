variable "subnets" {
  type = list(string)
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "namespace" {
  type = string
}

variable "cognito_user_pool_arn" {
  type = string
}

variable "auth_scopes" {
  type = list(string)
}

variable "alarm_topic_arn" {
  type = string
}

variable "static_content_bucket_name" {
  type = string
}

variable "domain_name" {
  type = string
}

variable "cert_domain_name" {
  type = string
}