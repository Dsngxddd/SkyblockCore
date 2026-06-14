package net.cengiz1.skyblock.invite;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InviteManager {

    private static class Invite {
        final UUID islandId;
        final UUID inviter;
        final long expiresAt;

        Invite(UUID islandId, UUID inviter, long expiresAt) {
            this.islandId = islandId;
            this.inviter = inviter;
            this.expiresAt = expiresAt;
        }
    }

    private final long expireMillis;
    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();

    public InviteManager(int expireSeconds) {
        this.expireMillis = expireSeconds * 1000L;
    }

    public void invite(UUID target, UUID islandId, UUID inviter) {
        this.invites.put(target, new Invite(islandId, inviter, System.currentTimeMillis() + this.expireMillis));
    }

    public boolean hasInvite(UUID target) {
        Invite invite = this.invites.get(target);
        if (invite == null)
            return false;
        if (System.currentTimeMillis() > invite.expiresAt) {
            this.invites.remove(target);
            return false;
        }
        return true;
    }

    public UUID consume(UUID target) {
        Invite invite = this.invites.remove(target);
        if (invite == null || System.currentTimeMillis() > invite.expiresAt)
            return null;
        return invite.islandId;
    }

    public UUID getInviteIsland(UUID target) {
        Invite invite = this.invites.get(target);
        return invite == null ? null : invite.islandId;
    }

    public void cancel(UUID target) {
        this.invites.remove(target);
    }
}
