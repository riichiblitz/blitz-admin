package com.yufimtsev.tenhou.discord;

import com.yufimtsev.tenhou.clouds.logger.Log;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class DiscordBot {

    private static final String TAG = "DiscordBot";
    private static final String BOT_TOKEN = "MzAxMjkzNzY2NzU5ODA5MDI0.C9Iiog._-AMS2mqYWpfXJxsoTJhnajeFMA";
    private IDiscordClient client;
    private IGuild guild;
    private IVoiceChannel muteChannel;
    private IRole everyoneRole;
    private HashMap<String, IUser> tenhouAliases;
    private HashMap<String, Pair<String, String>> tempAliases;

    private static DiscordBot instance;

    private DiscordBot() {
    }

    public static DiscordBot getInstance() {
        if (instance == null) {
            instance = new DiscordBot();
        }
        return instance;
    }

    public synchronized void start() {
        if (client != null && client.isLoggedIn()) {
            return;
        }
        try {
            client = getClient(BOT_TOKEN, true);
            if (tempAliases != null) {
                for (String tenhouName : tempAliases.keySet()) {
                    Pair<String, String> discordCredentials = tempAliases.get(tenhouName);
                    setAlias(tenhouName, discordCredentials.getKey(), discordCredentials.getValue());
                }
                tempAliases = null;
            }
        } catch (DiscordException e) {
            Log.d(TAG, e);
        }
    }

    public void stop() {
        if (client == null) {
            return;
        }

        try {
            client.logout();
        } catch (DiscordException e) {
            Log.d(TAG, e);
        }

        client = null;
    }

    public synchronized void setAlias(String tenhouName, String discordName, String discriminator) {
        if (client == null) {
            if (tempAliases == null) {
                tempAliases = new HashMap<>();
            }
            tempAliases.put(tenhouName, new Pair<>(discordName, discriminator));
            return;
        }
        IUser alias = findUserByName(discordName, discriminator);
        if (alias != null) {
            setAlias(tenhouName, alias);
        }
    }

    private IUser findUserByName(String discordName, String discriminator) {
        for (IUser user : client.getUsers()) {
            if (user.getName().equals(discordName) && user.getDiscriminator().equals(discriminator)) {
                return user;
            }
        }
        return null;
    }

    public void gameStarted(int table, List<String> tenhouNames) {
        checkClient();
        IVoiceChannel channel = null;
        
        for (String tenhouName : tenhouNames) {
            IUser alias = getAlias(tenhouName);
            if (alias != null) {
                if (channel == null) {
                    channel = createVoiceChannel("table-" + table);
                }
                
                moveUserToChannel(alias, channel);
            }
        }
        
        if (channel != null) {
            forbidVoiceChannel(channel);
        }
    }
    
    public void gameEnded(int table) {
        List<IVoiceChannel> channels = getGuild().getVoiceChannelsByName("table-" + table);
        if (channels.size() == 0) {
            return;
        }
        for (IVoiceChannel channel : channels) {
            for (IUser user : channel.getConnectedUsers()) {
                moveUserToChannel(user, getMuteChannel());
            }
            try {
                channel.delete();
            } catch (MissingPermissionsException e) {
                Log.d(TAG, e);
            } catch (RateLimitException e) {
                Log.d(TAG, e);
            } catch (DiscordException e) {
                Log.d(TAG, e);
            }
        }
    }

    public void debugMoveToMute(String discordName, String discriminator) {
        IUser user = findUserByName(discordName, discriminator);
        if (user != null) {
            moveUserToChannel(user, getMuteChannel());
        }
    }

    private IDiscordClient getClient(String token, boolean login) throws DiscordException { // Returns an instance of the Discord client
        ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
        clientBuilder.withToken(token); // Adds the login info to the builder
        if (login) {
            return clientBuilder.login(); // Creates the client instance and logs the client in
        } else {
            return clientBuilder.build(); // Creates the client instance but it doesn't log the client in yet, you would have to call client.login() yourself
        }
    }

    private IVoiceChannel createVoiceChannel(String name) {
        try {
            return getGuild().createVoiceChannel(name);
        } catch (DiscordException e) {
            Log.d(TAG, e);
        } catch (MissingPermissionsException e) {
            Log.d(TAG, e);
        } catch (RateLimitException e) {
            Log.d(TAG, e);
        }
        return null;
    }
    
    private void forbidVoiceChannel(IVoiceChannel channel) {
        try {
            channel.overrideRolePermissions(getEveryoneRole(), null, EnumSet.of(Permissions.VOICE_CONNECT));
        } catch (MissingPermissionsException e) {
            Log.d(TAG, e);
        } catch (RateLimitException e) {
            Log.d(TAG, e);
        } catch (DiscordException e) {
            Log.d(TAG, e);
        }
    }
    
    private void moveUserToChannel(IUser user, IVoiceChannel channel) {
        checkClient();
        try {
            user.moveToVoiceChannel(channel);
        } catch (DiscordException e) {
            Log.d(TAG, e);
        } catch (RateLimitException e) {
            Log.d(TAG, e);
        } catch (MissingPermissionsException e) {
            Log.d(TAG, e);
        }
    }

    private void checkClient() {
        if (client == null) {
            throw new IllegalStateException();
        }
    }

    private void setAlias(String tenhouName, IUser alias) {
        if (tenhouAliases == null) {
            tenhouAliases = new HashMap<String, IUser>();
        }
        tenhouAliases.put(tenhouName, alias);
    }
    
    private IUser getAlias(String tenhouName) {
        if (tenhouAliases == null) {
            return null;
        }
        return tenhouAliases.get(tenhouName);
    }

    private IGuild getGuild() {
        if (guild == null) {
            guild = client.getGuilds().get(0);
        }
        return guild;
    }

    private IRole getEveryoneRole() {
        if (everyoneRole == null) {
            everyoneRole = client.getRoleByID("301292953882591232"); // yeah, magic number
        }
        return everyoneRole;
    }

    private IVoiceChannel getMuteChannel() {
        if (muteChannel == null) {
            muteChannel = getGuild().getVoiceChannelsByName("mute").get(0);
        }
        return muteChannel;
    }

}
