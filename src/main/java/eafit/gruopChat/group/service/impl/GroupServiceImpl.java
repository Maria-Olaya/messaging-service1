package eafit.gruopChat.group.service.impl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import eafit.gruopChat.grpc.UserGrpcClient;
import eafit.gruopChat.grpc.UserResponse;
import eafit.gruopChat.group.dto.ChannelRequestDTO;
import eafit.gruopChat.group.dto.ChannelResponseDTO;
import eafit.gruopChat.group.dto.GroupMemberResponseDTO;
import eafit.gruopChat.group.dto.GroupRequestDTO;
import eafit.gruopChat.group.dto.GroupResponseDTO;
import eafit.gruopChat.group.dto.InvitationResponseDTO;
import eafit.gruopChat.group.exception.AlreadyMemberException;
import eafit.gruopChat.group.exception.ChannelNotFoundException;
import eafit.gruopChat.group.exception.DuplicateChannelNameException;
import eafit.gruopChat.group.exception.GroupNotFoundException;
import eafit.gruopChat.group.exception.InvitationNotFoundException;
import eafit.gruopChat.group.exception.NotGroupAdminException;
import eafit.gruopChat.group.exception.NotMemberException;
import eafit.gruopChat.group.model.Channel;
import eafit.gruopChat.group.model.Group;
import eafit.gruopChat.group.model.GroupInvitation;
import eafit.gruopChat.group.model.GroupMember;
import eafit.gruopChat.group.repository.ChannelRepository;
import eafit.gruopChat.group.repository.GroupInvitationRepository;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.group.repository.GroupRepository;
import eafit.gruopChat.group.service.GroupService;
import eafit.gruopChat.messaging.repository.MessageReceiptRepository;
import eafit.gruopChat.messaging.repository.MessageRepository;
import eafit.gruopChat.presence.repository.MessageReadRepository;
import eafit.gruopChat.shared.enums.GroupRole;
import eafit.gruopChat.shared.enums.InvitationStatus;
import eafit.gruopChat.user.exception.UserNotFoundException;
@Service
@Transactional
public class GroupServiceImpl implements GroupService {
    private final GroupRepository           groupRepository;
    private final GroupMemberRepository     memberRepository;
    private final ChannelRepository         channelRepository;
    private final GroupInvitationRepository invitationRepository;
    private final UserGrpcClient            userGrpcClient;
    private final MessageRepository         messageRepository;
    private final MessageReceiptRepository  messageReceiptRepository;
    private final MessageReadRepository     messageReadRepository;
    public GroupServiceImpl(GroupRepository groupRepository, GroupMemberRepository memberRepository,
                            ChannelRepository channelRepository, GroupInvitationRepository invitationRepository,
                            UserGrpcClient userGrpcClient, MessageRepository messageRepository,
                            MessageReceiptRepository messageReceiptRepository, MessageReadRepository messageReadRepository) {
        this.groupRepository          = groupRepository;
        this.memberRepository         = memberRepository;
        this.channelRepository        = channelRepository;
        this.invitationRepository     = invitationRepository;
        this.userGrpcClient           = userGrpcClient;
        this.messageRepository        = messageRepository;
        this.messageReceiptRepository = messageReceiptRepository;
        this.messageReadRepository    = messageReadRepository;
    }
    @Override
    public GroupResponseDTO createGroup(Long creatorUserId, GroupRequestDTO request) {
        assertUserExists(creatorUserId);
        Group group = new Group();
        group.setName(request.name());
        group.setDescription(request.description());
        group.setCreatedBy(creatorUserId);
        group.setPrivate(request.isPrivate());
        Group saved = groupRepository.save(group);
        GroupMember adminMember = new GroupMember();
        adminMember.setGroup(saved);
        adminMember.setUserId(creatorUserId);
        adminMember.setRole(GroupRole.ADMIN);
        memberRepository.save(adminMember);
        return mapGroupToDTO(saved);
    }
    @Override @Transactional(readOnly = true)
    public GroupResponseDTO getGroupById(Long groupId) { return mapGroupToDTO(findGroup(groupId)); }
    @Override @Transactional(readOnly = true)
    public List<GroupResponseDTO> getGroupsByMember(Long userId) {
        return groupRepository.findGroupsByMemberUserId(userId).stream().map(this::mapGroupToDTO).collect(Collectors.toList());
    }
    @Override
    public GroupResponseDTO updateGroup(Long groupId, Long requestingUserId, GroupRequestDTO request) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, requestingUserId);
        group.setName(request.name());
        group.setDescription(request.description());
        group.setPrivate(request.isPrivate());
        return mapGroupToDTO(group);
    }
    @Override
    public void deleteGroup(Long groupId, Long requestingUserId) {
        Group group = findGroup(groupId);
        if (!group.getCreatedBy().equals(requestingUserId)) throw new NotGroupAdminException();
        messageReadRepository.deleteByGroupId(groupId);
        messageReceiptRepository.deleteByGroupId(groupId);
        messageRepository.deleteByGroupId(groupId);
        groupRepository.delete(group);
    }
    @Override @Transactional(readOnly = true)
    public GroupResponseDTO getGroupByInviteCode(String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode).orElseThrow(() -> new GroupNotFoundException(-1L));
        if (group.isPrivate()) throw new IllegalArgumentException("Este grupo es privado");
        return mapGroupToDTO(group);
    }
    @Override
    public GroupResponseDTO joinByInviteCode(String inviteCode, Long userId) {
        Group group = groupRepository.findByInviteCode(inviteCode).orElseThrow(() -> new GroupNotFoundException(-1L));
        if (group.isPrivate()) throw new IllegalArgumentException("Este grupo es privado");
        if (memberRepository.existsByGroupGroupIdAndUserId(group.getGroupId(), userId)) return mapGroupToDTO(group);
        assertUserExists(userId);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUserId(userId);
        member.setRole(GroupRole.MEMBER);
        memberRepository.save(member);
        return mapGroupToDTO(group);
    }
    @Override @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> getMembers(Long groupId) {
        findGroup(groupId);
        return memberRepository.findByGroupGroupId(groupId).stream().map(this::mapMemberToDTO).collect(Collectors.toList());
    }
    @Override
    public void removeMember(Long groupId, Long adminUserId, Long targetUserId) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        if (group.getCreatedBy().equals(targetUserId)) throw new IllegalArgumentException("Cannot remove the group creator");
        memberRepository.deleteByGroupGroupIdAndUserId(groupId, targetUserId);
    }
    @Override
    public void changeGroupRole(Long groupId, Long adminUserId, Long targetUserId, GroupRole newRole) {
        findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        GroupMember member = memberRepository.findByGroupGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotMemberException(targetUserId, groupId));
        member.setRole(newRole);
    }
    @Override
    public void leaveGroup(Long groupId, Long userId) {
        Group group = findGroup(groupId);
        if (group.getCreatedBy().equals(userId)) throw new IllegalArgumentException("Group creator cannot leave.");
        if (!memberRepository.existsByGroupGroupIdAndUserId(groupId, userId)) throw new NotMemberException(userId, groupId);
        memberRepository.deleteByGroupGroupIdAndUserId(groupId, userId);
    }
    @Override
    public InvitationResponseDTO sendInvitation(Long groupId, Long adminUserId, Long invitedUserId) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        assertUserExists(invitedUserId);
        if (memberRepository.existsByGroupGroupIdAndUserId(groupId, invitedUserId)) throw new AlreadyMemberException(invitedUserId, groupId);
        invitationRepository.findByGroupGroupIdAndInvitedUserIdAndStatus(groupId, invitedUserId, InvitationStatus.PENDING)
                .ifPresent(i -> { throw new AlreadyMemberException(invitedUserId, groupId); });
        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setInvitedBy(adminUserId);
        invitation.setInvitedUserId(invitedUserId);
        invitation.setStatus(InvitationStatus.PENDING);
        return mapInvitationToDTO(invitationRepository.save(invitation));
    }
    @Override
    public InvitationResponseDTO respondToInvitation(Long invitationId, Long userId, boolean accept) {
        GroupInvitation invitation = invitationRepository.findById(invitationId).orElseThrow(() -> new InvitationNotFoundException(invitationId));
        if (!invitation.getInvitedUserId().equals(userId)) throw new NotGroupAdminException();
        if (invitation.getStatus() != InvitationStatus.PENDING) throw new IllegalArgumentException("Invitation already responded");
        invitation.setStatus(accept ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        if (accept) {
            GroupMember member = new GroupMember();
            member.setGroup(invitation.getGroup());
            member.setUserId(invitation.getInvitedUserId());
            member.setRole(GroupRole.MEMBER);
            memberRepository.save(member);
        }
        return mapInvitationToDTO(invitation);
    }
    @Override @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getPendingInvitations(Long userId) {
        return invitationRepository.findByInvitedUserIdAndStatus(userId, InvitationStatus.PENDING)
                .stream().map(this::mapInvitationToDTO).collect(Collectors.toList());
    }
    @Override
    public ChannelResponseDTO createChannel(Long groupId, Long adminUserId, ChannelRequestDTO request) {
        Group group = findGroup(groupId);
        assertAdmin(groupId, adminUserId);
        if (channelRepository.existsByGroupGroupIdAndName(groupId, request.name())) throw new DuplicateChannelNameException(request.name());
        Channel channel = new Channel();
        channel.setGroup(group);
        channel.setName(request.name());
        channel.setDescription(request.description());
        channel.setCreatedBy(adminUserId);
        return mapChannelToDTO(channelRepository.save(channel));
    }
    @Override @Transactional(readOnly = true)
    public List<ChannelResponseDTO> getChannels(Long groupId) {
        findGroup(groupId);
        return channelRepository.findByGroupGroupId(groupId).stream().map(this::mapChannelToDTO).collect(Collectors.toList());
    }
    @Override
    public ChannelResponseDTO updateChannel(Long channelId, Long adminUserId, ChannelRequestDTO request) {
        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new ChannelNotFoundException(channelId));
        assertAdmin(channel.getGroup().getGroupId(), adminUserId);
        channel.setName(request.name());
        channel.setDescription(request.description());
        return mapChannelToDTO(channel);
    }
    @Override
    public void deleteChannel(Long channelId, Long adminUserId) {
        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new ChannelNotFoundException(channelId));
        assertAdmin(channel.getGroup().getGroupId(), adminUserId);
        messageReadRepository.deleteByChannelId(channelId);
        messageReceiptRepository.deleteByChannelId(channelId);
        messageRepository.deleteByChannelId(channelId);
        channelRepository.delete(channel);
    }
    private void assertUserExists(Long userId) {
        if (!userGrpcClient.existsUser(String.valueOf(userId))) throw new UserNotFoundException(userId);
    }
    private Group findGroup(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
    }
    private void assertAdmin(Long groupId, Long userId) {
        memberRepository.findByGroupGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getRole() == GroupRole.ADMIN)
                .orElseThrow(NotGroupAdminException::new);
    }
    private GroupResponseDTO mapGroupToDTO(Group g) {
        UserResponse creator = userGrpcClient.getUserById(String.valueOf(g.getCreatedBy())).orElse(null);
        String creatorName = creator != null ? creator.getUsername() : null;
        return new GroupResponseDTO(g.getGroupId(), g.getName(), g.getDescription(),
                g.getCreatedBy(), creatorName, g.isPrivate(),
                g.getMembers().size(), g.getChannels().size(), g.getCreatedAt(), g.getInviteCode());
    }
    private GroupMemberResponseDTO mapMemberToDTO(GroupMember m) {
        UserResponse u = userGrpcClient.getUserById(String.valueOf(m.getUserId())).orElse(null);
        return new GroupMemberResponseDTO(m.getUserId(),
                u != null ? u.getUsername() : null,
                u != null ? u.getEmail() : null,
                m.getRole(), m.getJoinedAt());
    }
    private InvitationResponseDTO mapInvitationToDTO(GroupInvitation i) {
        UserResponse invitedBy = userGrpcClient.getUserById(String.valueOf(i.getInvitedBy())).orElse(null);
        UserResponse invitedUser = userGrpcClient.getUserById(String.valueOf(i.getInvitedUserId())).orElse(null);
        return new InvitationResponseDTO(i.getInvitationId(), i.getGroup().getGroupId(), i.getGroup().getName(),
                i.getInvitedBy(), invitedBy != null ? invitedBy.getUsername() : null,
                i.getInvitedUserId(), invitedUser != null ? invitedUser.getUsername() : null,
                i.getStatus(), i.getSentAt(), i.getRespondedAt());
    }
    private ChannelResponseDTO mapChannelToDTO(Channel c) {
        UserResponse creator = userGrpcClient.getUserById(String.valueOf(c.getCreatedBy())).orElse(null);
        return new ChannelResponseDTO(c.getChannelId(), c.getGroup().getGroupId(), c.getName(), c.getDescription(),
                c.getCreatedBy(), creator != null ? creator.getUsername() : null, c.getCreatedAt());
    }
}
