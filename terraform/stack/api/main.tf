module "services" {
  source = "services"

  namespace = "${var.namespace}"

  namespace_id = "${var.namespace_id}"

  subnets    = ["${var.subnets}"]
  cluster_id = "${var.cluster_id}"
  vpc_id     = "${var.vpc_id}"
  nlb_arn    = "${module.nlb.arn}"

  desired_bags_api_count    = "${var.desired_bags_api_count}"
  desired_ingests_api_count = "${var.desired_ingests_api_count}"

  # Bags endpoint

  bags_container_image       = "${var.bags_container_image}"
  bags_container_port        = "${var.bags_container_port}"
  bags_env_vars              = "${var.bags_env_vars}"
  bags_env_vars_length       = "${var.bags_env_vars_length}"
  bags_nginx_container_image = "${var.bags_nginx_container_image}"
  bags_nginx_container_port  = "${var.bags_nginx_container_port}"
  bags_listener_port         = "${local.bags_listener_port}"

  # Ingests endpoint

  ingests_container_image        = "${var.ingests_container_image}"
  ingests_container_port         = "${var.ingests_container_port}"
  ingests_env_vars               = "${var.ingests_env_vars}"
  ingests_env_vars_length        = "${var.ingests_env_vars_length}"
  ingests_nginx_container_port   = "${var.ingests_nginx_container_port}"
  ingests_nginx_container_image  = "${var.ingests_nginx_container_image}"
  ingests_listener_port          = "${local.ingests_listener_port}"
  interservice_security_group_id = "${var.interservice_security_group_id}"
}
