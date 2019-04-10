package protocol.backup;

import channel.Channel;
import message.Message;
import peer.Chunk;
import peer.FileManager;
import peer.Peer;

import java.io.IOException;

public class Backup {

    private Message msg;
    private FileManager fm;
    private String path;
    private Chunk chunk;

    public Backup(Message msg) {

        System.out.println("PUTCHUNK received");

        this.msg = msg;
        this.fm = Peer.getInstance().getFileManager();


        if(msg.getSenderId() == Peer.getInstance().getId()) {
            return;
        }

        if (!this.fm.hasChunk(msg.getFileId(), msg.getChunkNo())) {
            path = Peer.getInstance().getBackupPath(msg.getFileId());
            this.fm.createFolder(path);
            start();
        } else {
            chunk = this.fm.getChunk(msg.getFileId(), msg.getChunkNo());
            sendSTORED();
        }

    }

    private void start() {
        chunk = new Chunk(this.msg.getFileId(), this.msg.getChunkNo(), this.msg.getReplicationDeg(), this.msg.getBody());
        if(saveChunk()) {
            sendSTORED();
        }

    }

    private boolean saveChunk() {
        boolean success;
        try {
            success = this.fm.saveFile(Integer.toString(chunk.getChunkNo()), path, chunk.getData());
        } catch (IOException e) {
            System.out.println("Error storing chunk");
            return false;
        }

        if(success) {
            this.fm.addChunk(chunk);
        } else {
            return false;
        }
        return true;
    }

    private void sendSTORED() {
        String[] args = {
                Peer.getInstance().getVersion(),
                Integer.toString(Peer.getInstance().getId()),
                chunk.getFileId(),
                Integer.toString(chunk.getChunkNo())
        };
        System.out.println("Stored");
        Message msg = new Message(Message.MessageType.STORED, args);
        Peer.getInstance().send(Channel.Type.MC, msg, true);
    }
}
