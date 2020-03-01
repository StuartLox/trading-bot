data "template_file" "aws_auth" {
  template = "${file("${path.module}/templates/aws-auth.yml")}"
}

resource "k8s_manifest" "aws_auth_config" {
  content = "${data.template_file.aws_auth.rendered}"
}
