// SES 수신 메일 포워더
// SES 수신 규칙이 원문을 S3(inbox/<messageId>)에 저장한 뒤 이 함수를 호출한다.
// 원문을 읽어 헤더만 손보고 FORWARD_TO 로 재전송한다.
//
// From 을 그대로 두면 SPF/DMARC 불일치로 반송되므로, From 은 우리 도메인 주소
// (FORWARD_FROM)로 바꾸고 원발신자는 Reply-To 로 보존한다. → 받은 메일에서 그대로
// "답장"하면 원발신자에게 간다.
import { S3Client, GetObjectCommand } from "@aws-sdk/client-s3";
import { SESClient, SendRawEmailCommand } from "@aws-sdk/client-ses";

const s3 = new S3Client({});
const ses = new SESClient({});

const BUCKET = process.env.MAIL_BUCKET;
const PREFIX = process.env.MAIL_PREFIX ?? "inbox/";
const FORWARD_TO = process.env.FORWARD_TO;
const FORWARD_FROM = process.env.FORWARD_FROM;

const streamToBuffer = async (stream) => {
  const chunks = [];
  for await (const chunk of stream) chunks.push(chunk);
  return Buffer.concat(chunks);
};

// 원문 헤더/바디를 분리해 From/Reply-To/To/Return-Path 를 재작성한다.
const rewrite = (raw) => {
  const sep = raw.indexOf("\r\n\r\n");
  const splitAt = sep === -1 ? raw.indexOf("\n\n") : sep;
  const headerBlock = raw.slice(0, splitAt);
  const body = raw.slice(splitAt);

  // 헤더 라인(폴딩 포함) 파싱
  const lines = headerBlock.split(/\r?\n/);
  const kept = [];
  let originalFrom = "";
  let buf = "";

  const flush = () => {
    if (!buf) return;
    const name = buf.slice(0, buf.indexOf(":")).trim().toLowerCase();
    if (name === "from") {
      originalFrom = buf.slice(buf.indexOf(":") + 1).trim();
    } else if (
      // 재서명/경로 관련 헤더는 제거 (재전송 시 무효)
      ["return-path", "sender", "dkim-signature", "reply-to", "to", "cc"].includes(name)
    ) {
      // drop
    } else {
      kept.push(buf);
    }
    buf = "";
  };

  for (const line of lines) {
    if (/^[ \t]/.test(line)) {
      buf += "\r\n" + line; // 폴딩된 연속 라인
    } else {
      flush();
      buf = line;
    }
  }
  flush();

  const fromName = originalFrom.replace(/<[^>]*>/, "").replace(/"/g, "").trim();
  const newFrom = fromName
    ? `${fromName} via hilit.my <${FORWARD_FROM}>`
    : `<${FORWARD_FROM}>`;

  const newHeaders = [
    `From: ${newFrom}`,
    `Reply-To: ${originalFrom || FORWARD_FROM}`,
    `To: ${FORWARD_TO}`,
    ...kept,
  ].join("\r\n");

  return newHeaders + body;
};

export const handler = async (event) => {
  const mail = event.Records[0].ses.mail;
  const messageId = mail.messageId;
  const key = `${PREFIX}${messageId}`;

  const obj = await s3.send(new GetObjectCommand({ Bucket: BUCKET, Key: key }));
  const raw = (await streamToBuffer(obj.Body)).toString("utf-8");

  const rewritten = rewrite(raw);

  await ses.send(
    new SendRawEmailCommand({
      Source: FORWARD_FROM,
      Destinations: [FORWARD_TO],
      RawMessage: { Data: Buffer.from(rewritten, "utf-8") },
    })
  );

  console.log(`forwarded ${messageId} -> ${FORWARD_TO}`);
  return { disposition: "STOP_RULE" };
};
