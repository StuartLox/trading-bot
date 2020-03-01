provider "k8s" {
  kubeconfig        = "${var.kube_config}"
  # kubeconfigContext = "arn:aws:eks:ap-southeast-2:224041885527:cluster/personal-eks"
}
