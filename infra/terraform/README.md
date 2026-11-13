# AWS deployment

This stack deploys the service across two Availability Zones with an internet-facing ALB and private ECS, RDS, MSK, and Redis resources. RDS manages its master password in Secrets Manager; Redis uses an authenticated TLS connection; ECS tasks authenticate to MSK Serverless with their IAM role.

## Bootstrap and deploy

1. Configure an encrypted remote state backend and AWS credentials outside this repository.
2. Create the ECR repository first: `terraform apply -target=aws_ecr_repository.app`.
3. Build and push the application image using the immutable commit SHA as the tag.
4. Run `terraform plan -var image_tag=<commit-sha> -out plan.tfplan` and review it.
5. Run `terraform apply plan.tfplan`.

The default production profile creates multi-AZ RDS, two Redis nodes, two ECS tasks, autoscaling to ten tasks, deployment rollback, Container Insights, encrypted storage, 14-day database backups, deletion protection, and SNS-backed alarms. Add an HTTPS listener and ACM certificate before exposing a real tenant API; port 80 exists only as a runnable infrastructure example.

The generated Redis credential is stored in encrypted Terraform state as well as Secrets Manager. Production state must therefore use encryption, access logging, locking, and tightly scoped IAM. A mature deployment should rotate the Redis secret through a controlled replacement workflow.
