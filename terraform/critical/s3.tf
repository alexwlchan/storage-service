resource "aws_s3_bucket" "static_content" {
  bucket = "wellcomecollection-public-${var.namespace}-static"
  acl    = "private"
}

resource "aws_s3_bucket" "ingests_drop" {
  bucket = "wellcomecollection-${var.namespace}-ingests"
  acl    = "private"
}

resource "aws_s3_bucket" "archive" {
  bucket = "wellcomecollection-${var.namespace}-archive"
  acl    = "private"

  lifecycle_rule {
    enabled = true

    transition {
      days          = 90
      storage_class = "GLACIER"
    }
  }
}

resource "aws_s3_bucket" "access" {
  bucket = "wellcomecollection-${var.namespace}-access"
  acl    = "private"
}

resource "aws_s3_bucket" "infra" {
  bucket = "wellcomecollection-${var.namespace}-infra"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  versioning {
    enabled = true
  }
}