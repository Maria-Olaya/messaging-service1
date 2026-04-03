package eafit.gruopChat.messaging.model;

import java.time.LocalDateTime;

import eafit.gruopChat.group.model.Channel;
import eafit.gruopChat.group.model.Group;
import eafit.gruopChat.shared.enums.MessageStatus;
import eafit.gruopChat.shared.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    // ID del usuario que envió el mensaje (sin FK a tabla users)
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // Nombre del usuario en el momento de enviar (desnormalizado)
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    // ===== Getters y Setters =====

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
}