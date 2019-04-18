package io.guthub.spafka.test.dto;



public class DataItem_1EDecoder extends BaseDecoder {
    public DataItem_1EDecoder() {
    }

    public static DataItem_1EDTO decode(byte[] b) {
        DataItem_1EDTO dto = new DataItem_1EDTO();
        StringBuilder dataBit = new StringBuilder();
        byte[] dataHead = new byte[DecoderDefine.BASE_HEAD_LENGTH];
        byte[] dataBody = new byte[DecoderDefine.BCM1E_BODY_LENGTH];
        System.arraycopy(b, 0, dataHead, 0, DecoderDefine.BASE_HEAD_LENGTH);
        System.arraycopy(b, DecoderDefine.BASE_HEAD_LENGTH, dataBody, 0, DecoderDefine.BCM1E_BODY_LENGTH);
        setCommon(dto, dataHead, b);

        int rRDoor;
        for(rRDoor = 0; rRDoor < dataBody.length; ++rRDoor) {
            dataBit.append(b2b.byteToBit(dataBody[rRDoor]));
        }

        rRDoor = b2b.binToSignedNumber(dataBit.substring(0, 2));
        if (rRDoor >= 0) {
            dto.setRRDoor(rRDoor);
        }

        int rLDoor = b2b.binToSignedNumber(dataBit.substring(2, 4));
        if (rLDoor >= 0) {
            dto.setRLDoor(rLDoor);
        }

        int fRDoor = b2b.binToSignedNumber(dataBit.substring(4, 6));
        if (fRDoor >= 0) {
            dto.setFRDoor(fRDoor);
        }

        int fLDoor = b2b.binToSignedNumber(dataBit.substring(6, 8));
        if (fLDoor >= 0) {
            dto.setFLDoor(fLDoor);
        }

        int rRDoorLock = b2b.binToSignedNumber(dataBit.substring(8, 10));
        if (rRDoorLock >= 0) {
            dto.setRRDoorLock(rRDoorLock);
        }

        int rLDoorLock = b2b.binToSignedNumber(dataBit.substring(10, 12));
        if (rLDoorLock >= 0) {
            dto.setRLDoorLock(rLDoorLock);
        }

        int fRDoorLock = b2b.binToSignedNumber(dataBit.substring(12, 14));
        if (fRDoorLock >= 0) {
            dto.setFRDoorLock(fRDoorLock);
        }

        int fLDoorLock = b2b.binToSignedNumber(dataBit.substring(14, 16));
        if (fLDoorLock >= 0) {
            dto.setFLDoorLock(fLDoorLock);
        }

        int centralLock = b2b.binToSignedNumber(dataBit.substring(16, 18));
        if (centralLock >= 0) {
            dto.setCentralLock(centralLock);
        }

        int leftHeatedSeat = b2b.binToSignedNumber(dataBit.substring(18, 21));
        if (leftHeatedSeat >= 0) {
            dto.setLeftHeatedSeat(leftHeatedSeat);
        }

        int rightHeatedSeat = b2b.binToSignedNumber(dataBit.substring(21, 24));
        if (rightHeatedSeat >= 0) {
            dto.setRightHeatedSeat(rightHeatedSeat);
        }

        int trunkLock = b2b.binToSignedNumber(dataBit.substring(24, 26));
        if (trunkLock >= 0) {
            dto.setTrunkLock(trunkLock);
        }

        int trunk = b2b.binToSignedNumber(dataBit.substring(26, 28));
        if (trunk >= 0) {
            dto.setTrunk(trunk);
        }

        int rLWindow = b2b.binToSignedNumber(dataBit.substring(28, 31));
        if (rLWindow >= 0) {
            dto.setRLWindow(rLWindow);
        }

        int fLWindow = b2b.binToSignedNumber(dataBit.substring(31, 34));
        if (fLWindow >= 0) {
            dto.setFLWindow(fLWindow);
        }

        int rRWindow = b2b.binToSignedNumber(dataBit.substring(34, 37));
        if (rRWindow >= 0) {
            dto.setRRWindow(rRWindow);
        }

        int fRWindow = b2b.binToSignedNumber(dataBit.substring(37, 40));
        if (fLWindow >= 0) {
            dto.setFRWindow(fRWindow);
        }

        dto.setBody(dataBody);
        return dto;
    }
}