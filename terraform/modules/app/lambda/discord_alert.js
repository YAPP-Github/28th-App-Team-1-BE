'use strict';

const zlib = require('zlib');

const DISCORD_WEBHOOK_URL = process.env.DISCORD_WEBHOOK_URL;
const MAX_DESCRIPTION_LENGTH = 3500;

// CloudWatch Logs Subscription Filter가 호출한다. event.awslogs.data는
// gzip 압축 후 base64 인코딩된 JSON으로, 필터 패턴({ $.level = "ERROR" })에
// 매칭된 로그 이벤트들을 담고 있다.
exports.handler = async (event) => {
    const payload = JSON.parse(
        zlib.gunzipSync(Buffer.from(event.awslogs.data, 'base64')).toString('utf8')
    );

    // Discord 웹훅은 레이트리밋이 낮아 병렬 전송 시 429가 발생하기 쉽고,
    // 순서 보장·에러 전파를 위해서도 이벤트 순서대로 순차 전송한다.
    for (const logEvent of payload.logEvents) {
        await sendToDiscord(toDiscordPayload(logEvent));
    }
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
