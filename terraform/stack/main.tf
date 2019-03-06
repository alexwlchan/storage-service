# archivist

module "archivist" {
  source = "../modules/service/worker+nvm"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  service_name                     = "${var.namespace}-archivist"

  max_capacity       = "${var.desired_archivist_count}"
  desired_task_count = "${var.desired_archivist_count}"

  env_vars = {
    queue_url              = "${module.archivist_input_queue.url}"
    queue_parallelism      = "${var.archivist_queue_parallelism}"
    archive_bucket         = "${var.archive_bucket_name}"
    next_service_topic_arn = "${module.archivist_output_topic.arn}"
    progress_topic_arn     = "${local.progress_topic}"
    JAVA_OPTS              = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-archivist"
  }

  env_vars_length = 6

  cpu    = "1900"
  memory = "14000"

  container_image = "${local.archivist_image}"
}

# bags aka registrar-async

module "bags" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  vpc_id                           = "${var.vpc_id}"
  service_name                     = "${var.namespace}-bags"

  env_vars = {
    queue_url          = "${module.bags_input_queue.url}"
    archive_bucket     = "${var.archive_bucket_name}"
    progress_topic_arn = "${local.progress_topic}"
    vhs_bucket_name    = "${var.vhs_archive_manifest_bucket_name}"
    vhs_table_name     = "${var.vhs_archive_manifest_table_name}"
    JAVA_OPTS          = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-bags"
  }

  env_vars_length = 6

  container_image = "${local.bags_image}"
}

# bag_replicator

module "bag_replicator" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
    "${aws_security_group.service_egress.id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-bag-replicator"

  env_vars = {
    queue_url               = "${module.bag_replicator_input_queue.url}"
    destination_bucket_name = "${var.access_bucket_name}"
    progress_topic_arn      = "${local.progress_topic}"
    outgoing_topic_arn      = "${module.bag_replicator_output_topic.arn}"
    JAVA_OPTS               = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-bag-replicator"
  }

  env_vars_length = 5

  container_image = "${local.bag_replicator_image}"
}

# bag_verifier

module "bag_verifier" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
    "${aws_security_group.service_egress.id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-bag-verifier"

  env_vars = {
    queue_url               = "${module.bag_verifier_input_queue.url}"
    destination_bucket_name = "${var.access_bucket_name}"
    progress_topic_arn      = "${local.progress_topic}"
    outgoing_topic_arn      = "${module.bag_verifier_output_topic.arn}"
    JAVA_OPTS               = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-bag-verifier"
  }

  env_vars_length = 5

  container_image = "${local.bag_verifier_image}"
}

# bag_unpacker

module "bag_unpacker" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
    "${aws_security_group.service_egress.id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-bag-unpacker"

  env_vars = {
    queue_url               = "${module.bag_unpacker_queue.url}"
    destination_bucket_name = "${var.ingest_bucket_name}"
    progress_topic_arn      = "${module.ingests_topic.arn}"
    outgoing_topic_arn      = "${module.bag_unpacker_output_topic.arn}"
    JAVA_OPTS               = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-bag-unpacker"
  }

  env_vars_length = 5

  container_image = "${local.bag_unpacker_image}"
}

# notifier

module "notifier" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
    "${aws_security_group.service_egress.id}",
  ]

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  cluster_id   = "${aws_ecs_cluster.cluster.id}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-notifier"

  env_vars = {
    context_url        = "https://api.wellcomecollection.org/storage/v1/context.json"
    notifier_queue_url = "${module.notifier_input_queue.url}"
    progress_topic_arn = "${local.progress_topic}"
    JAVA_OPTS          = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-notifier"
  }

  env_vars_length = 4

  container_image = "${local.notifier_image}"
}

# ingests aka progress-async

module "ingests" {
  source = "../modules/service/worker"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets      = "${var.private_subnets}"
  vpc_id       = "${var.vpc_id}"
  service_name = "${var.namespace}-ingests"

  env_vars = {
    queue_url                   = "${module.ingests_input_queue.url}"
    topic_arn                   = "${module.ingests_output_topic.arn}"
    archive_progress_table_name = "${var.ingests_table_name}"
    JAVA_OPTS                   = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-ingests"
  }

  env_vars_length = 4

  container_image = "${local.ingests_image}"
}

# Storage API

module "api" {
  source = "api"

  vpc_id     = "${var.vpc_id}"
  cluster_id = "${aws_ecs_cluster.cluster.id}"
  subnets    = "${var.private_subnets}"

  domain_name      = "${var.domain_name}"
  cert_domain_name = "${var.cert_domain_name}"

  namespace    = "${var.namespace}"
  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  desired_bags_api_count    = "${var.desired_bags_api_count}"
  desired_ingests_api_count = "${var.desired_ingests_api_count}"

  # Auth

  auth_scopes = [
    "${var.cognito_storage_api_identifier}/ingests",
    "${var.cognito_storage_api_identifier}/bags",
  ]
  cognito_user_pool_arn = "${var.cognito_user_pool_arn}"

  # Bags endpoint

  bags_container_image = "${local.bags_api_image}"
  bags_container_port  = "9001"
  bags_env_vars = {
    context_url     = "${var.api_url}/context.json"
    app_base_url    = "${var.api_url}/storage/v1/bags"
    vhs_bucket_name = "${var.vhs_archive_manifest_bucket_name}"
    vhs_table_name  = "${var.vhs_archive_manifest_table_name}"
    JAVA_OPTS       = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-bags-api"
  }
  bags_env_vars_length       = 5
  bags_nginx_container_image = "${var.nginx_image}"
  bags_nginx_container_port  = "9000"

  # Ingests endpoint

  ingests_container_image = "${local.ingests_api_image}"
  ingests_container_port  = "9001"
  ingests_env_vars = {
    context_url                     = "${var.api_url}/context.json"
    app_base_url                    = "${var.api_url}/storage/v1/ingests"
    archivist_topic_arn             = "${module.ingest_requests_topic.arn}"
    unpacker_topic_arn              = "${module.bag_unpacker_topic.arn}"
    archive_progress_table_name     = "${var.ingests_table_name}"
    archive_bag_progress_index_name = "${var.ingests_table_progress_index_name}"
    JAVA_OPTS                       = "-Dcom.amazonaws.sdk.enableDefaultMetrics=cloudwatchRegion=${var.aws_region},metricNameSpace=${var.namespace}-ingests-api"
  }
  ingests_env_vars_length        = 7
  ingests_nginx_container_image  = "${var.nginx_image}"
  ingests_nginx_container_port   = "9000"
  static_content_bucket_name     = "${var.static_content_bucket_name}"
  interservice_security_group_id = "${aws_security_group.interservice.id}"
  alarm_topic_arn                = "${var.alarm_topic_arn}"
  bag_unpacker_topic_arn         = "${module.bag_unpacker_topic.arn}"
}

# Migration services

module "bagger" {
  source = "../modules/service/worker+nvm"

  service_egress_security_group_id = "${aws_security_group.service_egress.id}"
  cluster_name                     = "${aws_ecs_cluster.cluster.name}"
  cluster_id                       = "${aws_ecs_cluster.cluster.id}"
  namespace_id                     = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  subnets                          = "${var.private_subnets}"
  service_name                     = "${var.namespace}-bagger"

  env_vars = {
    METS_BUCKET_NAME         = "${var.bagger_mets_bucket_name}"
    READ_METS_FROM_FILESHARE = "${var.bagger_read_mets_from_fileshare}"
    WORKING_DIRECTORY        = "${var.bagger_working_directory}"

    DROP_BUCKET_NAME           = "${var.s3_bagger_drop_name}"
    DROP_BUCKET_NAME_METS_ONLY = "${var.s3_bagger_drop_mets_only_name}"
    DROP_BUCKET_NAME_ERRORS    = "${var.s3_bagger_errors_name}"

    CURRENT_PRESERVATION_BUCKET = "${var.bagger_current_preservation_bucket}"
    DLCS_SOURCE_BUCKET          = "${var.bagger_dlcs_source_bucket}"
    BAGGING_QUEUE               = "${module.bagger_queue.name}"
    BAGGING_COMPLETE_TOPIC_ARN  = "${module.bagging_complete_topic.arn}"

    // This value is hard coded as it is not provisioned via terraform
    DYNAMO_TABLE = "${var.bagger_progress_table}"

    AWS_DEFAULT_REGION = "${var.aws_region}"

    # DLCS config
    DLCS_ENTRY       = "${var.bagger_dlcs_entry}"
    DLCS_CUSTOMER_ID = "${var.bagger_dlcs_customer_id}"
    DLCS_SPACE       = "${var.bagger_dlcs_space}"

    # DDS credentials
    DDS_ASSET_PREFIX = "${var.bagger_dds_asset_prefix}"

    ARCHIVE_FORMAT = "zip"
  }

  env_vars_length = 17

  secret_env_vars = {
    DLCS_API_KEY    = "storage/bagger_dlcs_api_key"
    DLCS_API_SECRET = "storage/bagger_dlcs_api_secret"

    DDS_API_KEY    = "storage/bagger_dds_api_key"
    DDS_API_SECRET = "storage/bagger_dds_api_secret"
  }

  secret_env_vars_length = 4

  cpu    = "1900"
  memory = "14000"

  min_capacity = "${var.desired_bagger_count}"
  max_capacity = "${var.desired_bagger_count}"

  desired_task_count = "${var.desired_bagger_count}"

  container_image = "${local.bagger_image}"
}

module "trigger_bag_ingest" {
  source = "trigger_bag_ingest"

  name                   = "${var.namespace}-trigger-bag-ingest"
  lambda_s3_key          = "lambdas/archive/lambdas/trigger_bag_ingest.zip"
  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
  oauth_details_enc      = "${var.archive_oauth_details_enc}"
  bag_paths              = "${var.bag_paths}"
  ingest_bucket_name     = "${var.ingest_bucket_name}"

  use_encryption_key_policy = "${var.use_encryption_key_policy}"
}
