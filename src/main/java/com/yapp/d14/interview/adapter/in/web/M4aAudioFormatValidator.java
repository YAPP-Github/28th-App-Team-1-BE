package com.yapp.d14.interview.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// content-type·파일명 확장자는 클라이언트가 임의로 붙일 수 있어 신뢰하지 않고, 업로드된 바이트 자체가
// 실제 MP4 컨테이너에 AAC 오디오 트랙을 담고 있는지 얕게 파싱해 확인한다. 완전한 스펙 준수 디먹서가 아니라
// ftyp/moov 존재, 각 트랙의 핸들러(soun/vide), 오디오 트랙의 샘플 엔트리(mp4a)만 본다 — 비디오 트랙이 하나라도
// 있으면(video/mp4를 audio/mp4로 위장한 경우) 거부한다.
final class M4aAudioFormatValidator {

    private M4aAudioFormatValidator() {
    }

    static boolean isM4aAudio(byte[] bytes) {
        if (bytes == null || bytes.length < 16) {
            return false;
        }
        try {
            List<Box> topLevel = parseBoxes(bytes, 0, bytes.length);
            boolean hasFtyp = topLevel.stream().anyMatch(b -> b.type().equals("ftyp"));
            Box moov = topLevel.stream().filter(b -> b.type().equals("moov")).findFirst().orElse(null);
            if (!hasFtyp || moov == null) {
                return false;
            }

            List<Box> traks = parseBoxes(bytes, moov.contentStart(), moov.contentEnd()).stream()
                    .filter(b -> b.type().equals("trak"))
                    .toList();
            if (traks.isEmpty()) {
                return false;
            }

            boolean hasAacAudioTrack = false;
            for (Box trak : traks) {
                String handlerType = handlerType(bytes, trak);
                if ("vide".equals(handlerType)) {
                    return false;
                }
                if ("soun".equals(handlerType) && "mp4a".equals(sampleEntryFourCc(bytes, trak))) {
                    hasAacAudioTrack = true;
                }
            }
            return hasAacAudioTrack;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String handlerType(byte[] data, Box trak) {
        Box mdia = findChild(data, trak, "mdia");
        Box hdlr = mdia == null ? null : findChild(data, mdia, "hdlr");
        if (hdlr == null || hdlr.contentEnd() - hdlr.contentStart() < 12) {
            return null;
        }
        return new String(data, hdlr.contentStart() + 8, 4, StandardCharsets.US_ASCII);
    }

    private static String sampleEntryFourCc(byte[] data, Box trak) {
        Box mdia = findChild(data, trak, "mdia");
        Box minf = mdia == null ? null : findChild(data, mdia, "minf");
        Box stbl = minf == null ? null : findChild(data, minf, "stbl");
        Box stsd = stbl == null ? null : findChild(data, stbl, "stsd");
        if (stsd == null || stsd.contentEnd() - stsd.contentStart() < 16) {
            return null;
        }
        return new String(data, stsd.contentStart() + 12, 4, StandardCharsets.US_ASCII);
    }

    private static Box findChild(byte[] data, Box parent, String type) {
        return parseBoxes(data, parent.contentStart(), parent.contentEnd()).stream()
                .filter(b -> b.type().equals(type))
                .findFirst()
                .orElse(null);
    }

    // size(4)+type(4) 박스 헤더를 순회한다. size==1이면 뒤이은 8바이트 largesize를 실제 크기로 쓰고,
    // size==0이면 박스가 부모 끝까지 이어진다(mdat 등 마지막 박스에서만 유효).
    private static List<Box> parseBoxes(byte[] data, int start, int end) {
        List<Box> boxes = new ArrayList<>();
        int pos = start;
        while (pos + 8 <= end) {
            long size = readUint32(data, pos);
            String type = new String(data, pos + 4, 4, StandardCharsets.US_ASCII);
            int headerSize = 8;
            long contentEnd;
            if (size == 1) {
                if (pos + 16 > end) {
                    break;
                }
                headerSize = 16;
                contentEnd = pos + readUint64(data, pos + 8);
            } else if (size == 0) {
                contentEnd = end;
            } else {
                contentEnd = pos + size;
            }
            if (contentEnd > end || contentEnd < pos + headerSize) {
                break;
            }
            boxes.add(new Box(type, pos + headerSize, (int) contentEnd));
            pos = (int) contentEnd;
        }
        return boxes;
    }

    private static long readUint32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static long readUint64(byte[] data, int offset) {
        return (readUint32(data, offset) << 32) | readUint32(data, offset + 4);
    }

    private record Box(String type, int contentStart, int contentEnd) {
    }
}
