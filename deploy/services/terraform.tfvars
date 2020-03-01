terragrunt = {
  remote_state {
    backend = "s3"
    config = {
      bucket         = "personal-account-terraform-state"
      key            = "${get_env("TF_VAR_environment", "")}/trading-bot.tfstate"
      region         = "${get_env("TF_VAR_region","ap-southeast-2")}"
      encrypt        = true
      dynamodb_table = "personal-account-terraform-state-lock"
    }
  }
  terraform {
    
    extra_arguments "environment_vars" {
      arguments = [
        "-var-file=./${get_env("TF_VAR_account", "data-nonprod")}.tfvars",
      ]
      commands = ["${get_terraform_commands_that_need_vars()}"]
    }
  }
}