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

# ──────────────────────────────────────────────────────────────
# 리소스 알람 → SNS → discord_alert Lambda → Discord
# CloudWatch Alarm이 SNS 토픽에 상태 전이(ALARM/OK)를 게시하고, 위 discord_alert
# Lambda가 이를 구독해 포맷·전송한다. (로그 알림과 창구 통일)
# ──────────────────────────────────────────────────────────────
resource "aws_sns_topic" "alarms" {
  name = "${var.project_name}-alarms"
  tags = { Name = "${var.project_name}-alarms" }
}

resource "aws_sns_topic_subscription" "alarms_to_discord" {
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.discord_alert.arn

  # SNS가 구독 후 Lambda를 호출하려면 invoke 권한이 먼저 있어야 하므로 순서를 고정한다.
  depends_on = [aws_lambda_permission.allow_sns_alarms]
}

resource "aws_lambda_permission" "allow_sns_alarms" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.discord_alert.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.alarms.arn
}

# 커스텀 메트릭 알람은 CloudWatch Agent가 발행하는 InstanceId dimension으로 매칭한다.
locals {
  instance_id  = aws_spot_instance_request.app.spot_instance_id
  alarm_topics = [aws_sns_topic.alarms.arn]
}

# ── 커스텀 메트릭(D14/Host) 알람 ──
resource "aws_cloudwatch_metric_alarm" "mem_high" {
  alarm_name        = "${var.project_name}-mem-high"
  alarm_description = "메모리 사용률 85% 초과가 5분 지속"
  namespace         = "D14/Host"
  metric_name       = "mem_used_percent"
  dimensions        = { InstanceId = local.instance_id }

  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 5
  threshold           = 85
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "missing"

  alarm_actions             = local.alarm_topics
  ok_actions                = local.alarm_topics
  insufficient_data_actions = local.alarm_topics
}

resource "aws_cloudwatch_metric_alarm" "swap_high" {
  alarm_name        = "${var.project_name}-swap-high"
  alarm_description = "스왑 사용률 50% 초과가 5분 지속"
  namespace         = "D14/Host"
  metric_name       = "swap_used_percent"
  dimensions        = { InstanceId = local.instance_id }

  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 5
  threshold           = 50
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "missing"

  alarm_actions             = local.alarm_topics
  ok_actions                = local.alarm_topics
  insufficient_data_actions = local.alarm_topics
}

resource "aws_cloudwatch_metric_alarm" "disk_full" {
  alarm_name        = "${var.project_name}-disk-full"
  alarm_description = "루트(/) 디스크 사용률 80% 초과"
  namespace         = "D14/Host"
  metric_name       = "disk_used_percent"
  dimensions = {
    InstanceId = local.instance_id
    path       = "/"
    fstype     = var.disk_fstype
  }

  statistic           = "Average"
  period              = 60
  evaluation_periods  = 1
  datapoints_to_alarm = 1
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "missing"

  alarm_actions             = local.alarm_topics
  ok_actions                = local.alarm_topics
  insufficient_data_actions = local.alarm_topics
}

# ── 네이티브(AWS/EC2) 메트릭 알람 ──
resource "aws_cloudwatch_metric_alarm" "cpu_credit_low" {
  alarm_name        = "${var.project_name}-cpu-credit-low"
  alarm_description = "CPU 크레딧 잔량이 ${var.cpu_credit_low_threshold} 미만으로 15분 지속"
  namespace         = "AWS/EC2"
  metric_name       = "CPUCreditBalance"
  dimensions        = { InstanceId = local.instance_id }

  # CPUCreditBalance는 5분 간격으로 발행되므로 300초 × 3 = 15분
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  datapoints_to_alarm = 3
  threshold           = var.cpu_credit_low_threshold
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "missing"

  alarm_actions             = local.alarm_topics
  ok_actions                = local.alarm_topics
  insufficient_data_actions = local.alarm_topics
}

resource "aws_cloudwatch_metric_alarm" "status_check" {
  alarm_name        = "${var.project_name}-status-check"
  alarm_description = "인스턴스/시스템 상태 검사 실패가 2분 지속"
  namespace         = "AWS/EC2"
  metric_name       = "StatusCheckFailed"
  dimensions        = { InstanceId = local.instance_id }

  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  datapoints_to_alarm = 2
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "missing"

  alarm_actions             = local.alarm_topics
  ok_actions                = local.alarm_topics
  insufficient_data_actions = local.alarm_topics
}
