package com.yapp.d14.interview.adapter.in.web;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

// M4aAudioFormatValidator가 파싱하는 필드만 채운, 테스트 전용 최소 MP4 박스 구조 빌더.
final class Mp4Fixtures {

    private Mp4Fixtures() {
    }

    static byte[] validM4aAudio() {
        return concat(ftyp(), moov(trak("soun", "mp4a")));
    }

    static byte[] videoTrack() {
        return concat(ftyp(), moov(trak("vide", "avc1")));
    }

    static byte[] nonAacAudioTrack() {
        return concat(ftyp(), moov(trak("soun", "alac")));
    }

    static byte[] notMp4() {
        // WebM(EBML) 매직 넘버 — MP4 박스 구조가 전혀 아닌 바이트.
        return new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    private static byte[] ftyp() {
        return box("ftyp", "isomiso2mp41".getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] moov(byte[]... traks) {
        return box("moov", concat(traks));
    }

    private static byte[] trak(String handlerType, String sampleEntryFourCc) {
        return box("trak", mdia(handlerType, sampleEntryFourCc));
    }

    private static byte[] mdia(String handlerType, String sampleEntryFourCc) {
        return box("mdia", concat(hdlr(handlerType), minf(sampleEntryFourCc)));
    }

    // version(1)+flags(3)+pre_defined(4) = 8바이트 뒤에 handler_type(4바이트)이 온다.
    private static byte[] hdlr(String handlerType) {
        byte[] content = new byte[24];
        System.arraycopy(handlerType.getBytes(StandardCharsets.US_ASCII), 0, content, 8, 4);
        return box("hdlr", content);
    }

    private static byte[] minf(String sampleEntryFourCc) {
        return box("minf", box("stbl", stsd(sampleEntryFourCc)));
    }

    // version(1)+flags(3)+entry_count(4) = 8바이트, 이어서 SampleEntry(size(4)+format(4)).
    private static byte[] stsd(String sampleEntryFourCc) {
        byte[] content = new byte[16];
        content[7] = 1; // entry_count = 1
        content[11] = 8; // sample entry size = 8 (헤더뿐, 추가 payload 없음)
        System.arraycopy(sampleEntryFourCc.getBytes(StandardCharsets.US_ASCII), 0, content, 12, 4);
        return box("stsd", content);
    }

    private static byte[] box(String type, byte[] content) {
        byte[] result = new byte[8 + content.length];
        int size = result.length;
        result[0] = (byte) (size >>> 24);
        result[1] = (byte) (size >>> 16);
        result[2] = (byte) (size >>> 8);
        result[3] = (byte) size;
        System.arraycopy(type.getBytes(StandardCharsets.US_ASCII), 0, result, 4, 4);
        System.arraycopy(content, 0, result, 8, content.length);
        return result;
    }

    private static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            out.writeBytes(array);
        }
        return out.toByteArray();
    }
}
