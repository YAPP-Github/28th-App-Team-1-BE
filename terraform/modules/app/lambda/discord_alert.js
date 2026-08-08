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

// 알람 이름 → 감시 대상(subject)과 경보 시 표현(problem). 알람 이름 끝부분(suffix)으로
// 매칭해 프로젝트 프리픽스(d14-)에 의존하지 않는다.
const ALARM_KO = {
    'mem-high': { subject: '메모리 사용률', problem: '높음' },
    'swap-high': { subject: '스왑 사용률', problem: '높음' },
    'disk-full': { subject: '디스크 사용량', problem: '높음' },
    'cpu-credit-low': { subject: 'CPU 크레딧', problem: '부족' },
    'status-check': { subject: '상태 검사', problem: '실패' },
};

const STATE_KO = { ALARM: '경보', OK: '정상', INSUFFICIENT_DATA: '데이터 부족' };

const OP_SYMBOL = {
    GreaterThanThreshold: '>',
    GreaterThanOrEqualToThreshold: '≥',
    LessThanThreshold: '<',
    LessThanOrEqualToThreshold: '≤',
};

// 제목은 상태에 따라 달라진다. 경보=문제(높음/부족/실패), 정상=정상 회복, 그 외=데이터 없음.
// 미등록 알람은 원래 이름을 그대로 쓴다.
function alarmTitleKo(alarmName, state) {
    let info = null;
    for (const [suffix, v] of Object.entries(ALARM_KO)) {
        if (alarmName.endsWith(suffix)) { info = v; break; }
    }
    if (!info) return alarmName;
    if (state === 'OK') return `${info.subject} 정상 회복`;
    if (state === 'ALARM') return `${info.subject} ${info.problem}`;
    return `${info.subject} 데이터 없음`;
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

    const stateKo = STATE_KO[state] || state || '-';
    const oldStateKo = STATE_KO[alarm.OldStateValue] || alarm.OldStateValue || '-';

    const trigger = alarm.Trigger || {};
    const fields = [
        { name: '상태', value: `${oldStateKo} → ${stateKo}`, inline: true },
    ];
    if (trigger.Namespace || trigger.MetricName) {
        fields.push({ name: '메트릭', value: `${trigger.Namespace || '-'} / ${trigger.MetricName || '-'}`, inline: true });
    }
    if (trigger.Threshold !== undefined && trigger.Threshold !== null) {
        const op = OP_SYMBOL[trigger.ComparisonOperator] || trigger.ComparisonOperator || '';
        fields.push({ name: '임계', value: `${op} ${trigger.Threshold}`.trim(), inline: true });
    }

    // 본문도 상태에 맞춰 한국어로 구성한다.
    // 경보일 때만 감시 조건(Terraform의 한국어 AlarmDescription)을 보여준다.
    // (CloudWatch가 주는 NewStateReason은 영어라 사용하지 않는다.)
    let description;
    if (state === 'OK') {
        description = '정상 범위로 복구되었습니다.';
    } else if (state === 'ALARM') {
        description = alarm.AlarmDescription || alarm.NewStateReason || '(설명 없음)';
    } else {
        description = '메트릭 데이터가 수신되지 않아 상태를 판단할 수 없습니다.';
    }

    return {
        embeds: [
            {
                title: `${emoji} ${alarmTitleKo(alarm.AlarmName, state)}`,
                description: truncate(description, MAX_DESCRIPTION_LENGTH),
                color,
                fields,
                // 알람 식별자(예: d14-mem-high)는 디버깅용으로 푸터에 남긴다.
                footer: { text: alarm.AlarmName },
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
