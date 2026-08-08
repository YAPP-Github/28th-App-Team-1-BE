'use strict';

const zlib = require('zlib');

const DISCORD_WEBHOOK_URL = process.env.DISCORD_WEBHOOK_URL;
const MAX_DESCRIPTION_LENGTH = 3500;

// 두 종류의 이벤트를 처리한다 (알림 창구를 하나로 통일).
//  1) CloudWatch Logs Subscription Filter → event.awslogs.data
//     gzip 압축 후 base64 인코딩된 JSON으로, 필터 패턴({ $.level = "ERROR" })에
//     매칭된 로그 이벤트들을 담고 있다.
//  2) CloudWatch Alarm → SNS → 이 Lambda → event.Records[].Sns
//     Sns.Message는 알람 상태 전이 JSON 문자열.
// Discord 웹훅은 레이트리밋이 낮아 병렬 전송 시 429가 발생하기 쉽고,
// 순서 보장·에러 전파를 위해서도 항상 순차 전송한다.
exports.handler = async (event) => {
    if (event.awslogs) {
        const payload = JSON.parse(
            zlib.gunzipSync(Buffer.from(event.awslogs.data, 'base64')).toString('utf8')
        );
        for (const logEvent of payload.logEvents) {
            await sendToDiscord(toDiscordPayload(logEvent));
        }
        return;
    }

    if (Array.isArray(event.Records)) {
        for (const record of event.Records) {
            if (record.Sns) {
                await sendToDiscord(toAlarmDiscordPayload(record.Sns));
            }
        }
        return;
    }

    console.warn('처리할 수 없는 이벤트 형식:', JSON.stringify(event));
};

function toDiscordPayload(logEvent) {
    let parsed;
    try {
        parsed = JSON.parse(logEvent.message);
    } catch (e) {
        parsed = { message: logEvent.message };
    }

    const fields = [];
    if (parsed.traceId) fields.push({ name: 'traceId', value: parsed.traceId, inline: true });
    if (parsed.userId) fields.push({ name: 'userId', value: parsed.userId, inline: true });
    if (parsed.logger_name) fields.push({ name: 'logger', value: parsed.logger_name, inline: false });

    let body = parsed.message || '(메시지 없음)';
    if (parsed.stack_trace) {
        body += '\n\n' + parsed.stack_trace;
    }

    return {
        embeds: [
            {
                title: `🔴 ${parsed.level || 'ERROR'} 발생`,
                description: '```\n' + truncate(body, MAX_DESCRIPTION_LENGTH) + '\n```',
                color: 15158332,
                fields,
                timestamp: new Date(logEvent.timestamp).toISOString(),
            },
        ],
    };
}

// SNS로 전달된 CloudWatch 알람 상태 전이를 Discord embed로 변환한다.
// Sns.Message는 알람 JSON 문자열이며, 상태(ALARM/OK/INSUFFICIENT_DATA)에 따라 색을 달리한다.
function toAlarmDiscordPayload(sns) {
    let alarm;
    try {
        alarm = JSON.parse(sns.Message);
    } catch (e) {
        alarm = null;
    }

    // 알람 형식이 아닌 SNS 메시지는 원문 그대로 전달한다.
    if (!alarm || !alarm.AlarmName) {
        return { content: truncate(sns.Message || sns.Subject || '(빈 SNS 메시지)', MAX_DESCRIPTION_LENGTH) };
    }

    const state = alarm.NewStateValue;
    const emoji = state === 'ALARM' ? '🚨' : state === 'OK' ? '✅' : '⚠️';
    // ALARM=빨강, OK=초록, INSUFFICIENT_DATA=주황
    const color = state === 'ALARM' ? 15158332 : state === 'OK' ? 3066993 : 15844367;

    const trigger = alarm.Trigger || {};
    const fields = [
        { name: '상태', value: `${alarm.OldStateValue || '-'} → ${state || '-'}`, inline: true },
    ];
    if (trigger.Namespace || trigger.MetricName) {
        fields.push({ name: '메트릭', value: `${trigger.Namespace || '-'} / ${trigger.MetricName || '-'}`, inline: true });
    }
    if (trigger.Threshold !== undefined && trigger.Threshold !== null) {
        fields.push({ name: '임계', value: `${trigger.ComparisonOperator || ''} ${trigger.Threshold}`.trim(), inline: true });
    }

    return {
        embeds: [
            {
                title: `${emoji} ${alarm.AlarmName}`,
                description: truncate(alarm.NewStateReason || '(사유 없음)', MAX_DESCRIPTION_LENGTH),
                color,
                fields,
                timestamp: alarm.StateChangeTime
                    ? new Date(alarm.StateChangeTime).toISOString()
                    : new Date().toISOString(),
            },
        ],
    };
}

function truncate(text, maxLength) {
    return text.length > maxLength ? text.slice(0, maxLength) + '\n... (truncated)' : text;
}

const MAX_RETRIES = 3;

async function sendToDiscord(body, attempt = 0) {
    const response = await fetch(DISCORD_WEBHOOK_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    if (response.status === 429 && attempt < MAX_RETRIES) {
        const retryAfterSeconds = Number(response.headers.get('retry-after')) || 1;
        await sleep(retryAfterSeconds * 1000);
        return sendToDiscord(body, attempt + 1);
    }

    if (!response.ok) {
        const text = await response.text();
        throw new Error(`Discord webhook 전송 실패 (${response.status}): ${text}`);
    }
}

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}
