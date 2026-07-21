# ──────────────────────────────────────────────────────────────
# 에러 로그 → Discord 알림
# CloudWatch Logs Subscription Filter가 aws_cloudwatch_log_group.app(main.tf)에서
# level=ERROR 로그를 실시간으로 Lambda에 전달하고, Lambda가 Discord 웹훅으로 포맷해 전송한다.
# ──────────────────────────────────────────────────────────────

data "archive_file" "discord_alert_lambda" {
  type        = "zip"
  source_file = "${path.module}/lambda/discord_alert.js"
  output_path = "${path.module}/lambda/discord_alert.zip"
}

resource "aws_iam_role" "discord_alert_lambda" {
  name = "${var.project_name}-discord-alert-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "discord_alert_lambda_logs" {
  role       = aws_iam_role.discord_alert_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "discord_alert" {
  function_name    = "${var.project_name}-discord-alert"
  role             = aws_iam_role.discord_alert_lambda.arn
  handler          = "discord_alert.handler"
  runtime          = "nodejs20.x"
  timeout          = 10
  memory_size      = 128
  filename         = data.archive_file.discord_alert_lambda.output_path
  source_code_hash = data.archive_file.discord_alert_lambda.output_base64sha256

  environment {
    variables = {
      DISCORD_WEBHOOK_URL = var.discord_webhook_url
    }
  }

  tags = { Name = "${var.project_name}-discord-alert" }
}

resource "aws_lambda_permission" "allow_cloudwatch_logs" {
  statement_id  = "AllowExecutionFromCloudWatchLogs"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.discord_alert.function_name
  principal     = "logs.amazonaws.com"
  source_arn    = "${aws_cloudwatch_log_group.app.arn}:*"
}

resource "aws_cloudwatch_log_subscription_filter" "app_errors" {
  name            = "${var.project_name}-app-errors-to-discord"
  log_group_name  = aws_cloudwatch_log_group.app.name
  filter_pattern  = "{ $.level = \"ERROR\" }"
  destination_arn = aws_lambda_function.discord_alert.arn

  depends_on = [aws_lambda_permission.allow_cloudwatch_logs]
}
