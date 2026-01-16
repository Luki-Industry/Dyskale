package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.application.service.LinkService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class RoleSyncService extends ListenerAdapter {

    @Inject
    private LuckPerms luckPerms;

    @Inject
    private DiscordBot discordBot;

    @Inject
    private LinkService linkService;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private Logger logger;

    private final Map<String, String> roleToGroupMap = new HashMap<>(); // RoleID -> GroupName

    public void init() {
        loadConfig();

        if (discordBot.getJda() != null) {
            discordBot.getJda().addEventListener(this);
            logger.log(Level.INFO, "RoleSyncService: Écouteurs Discord enregistrés.");
        } else {
             logger.log(Level.WARNING, "RoleSyncService: JDA est null, impossible d'enregistrer les écouteurs.");
        }
    }

    private void loadConfig() {
        String mappingString = config.get().getString("Role_Group_Mapping");
        roleToGroupMap.clear();

        if (mappingString == null || mappingString.isEmpty()) {
            return;
        }

        String[] mappings = mappingString.split(",");
        for (String mapping : mappings) {
            String[] parts = mapping.split(":");
            if (parts.length == 2) {
                String roleId = parts[0].trim();
                String groupName = parts[1].trim();
                if (!roleId.isEmpty() && !groupName.isEmpty()) {
                    roleToGroupMap.put(roleId, groupName);
                }
            }
        }
        logger.log(Level.INFO, "Loaded " + roleToGroupMap.size() + " role mappings for synchronization.");
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        processRoleChange(event.getMember(), event.getRoles(), true);
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        processRoleChange(event.getMember(), event.getRoles(), false);
    }

    private void processRoleChange(Member member, List<Role> roles, boolean add) {
        if (member.getUser().isBot()) {
            return;
        }

        UUID playerUuid = linkService.getPlayerUuid(member.getId());
        if (playerUuid == null) {
            return;
        }

        for (Role role : roles) {
            String groupName = roleToGroupMap.get(role.getId());
            if (groupName != null) {
                updateLuckPermsGroup(playerUuid, groupName, add);
            }
        }
    }

    private void updateLuckPermsGroup(UUID playerUuid, String groupName, boolean add) {
        luckPerms.getUserManager().loadUser(playerUuid).thenAcceptAsync(user -> {
            boolean changed = false;
            Node groupNode = InheritanceNode.builder(groupName).build();

            if (add) {
                if (user.data().add(groupNode).wasSuccessful()) {
                    changed = true;
                    logger.log(Level.INFO, "Sync Discord -> Hytale: Added group " + groupName + " to " + user.getFriendlyName());
                }
            } else {
                if (user.data().remove(groupNode).wasSuccessful()) {
                    changed = true;
                    logger.log(Level.INFO, "Sync Discord -> Hytale: Removed group " + groupName + " from " + user.getFriendlyName());
                }
            }

            if (changed) {
                luckPerms.getUserManager().saveUser(user);
            }
        });
    }

    // Sync all roles for a user (called when linking)
    public void syncUser(UUID playerUuid, String discordId) {
        Guild guild = discordBot.getGuild();
        if (guild != null) {
            guild.retrieveMemberById(discordId).queue(member -> {
                List<Role> roles = member.getRoles();
                for (Role role : roles) {
                     String groupName = roleToGroupMap.get(role.getId());
                     if (groupName != null) {
                         updateLuckPermsGroup(playerUuid, groupName, true);
                     }
                }
            });
        }
    }
}

