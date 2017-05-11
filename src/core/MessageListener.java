package core;

import nests.Nest;
import nests.NestSearch;
import nests.NestSheetManager;
import nests.NestStatus;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import notifier.Notifier;
import parser.*;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageListener extends ListenerAdapter
{
    private static final String NEST_FB_GROUP = "https://www.facebook.com/groups/PogoCBRNests/";
    private static final String NEST_MAP = "http://www.google.com/maps/d/u/0/viewer?mid=1d-QuaDK1tJRiHKODXErTQIDqIAY";
    private static TextChannel trello;
    private static TextChannel nestsReports;
    public static Guild cbrSightings;
    private static final ReporterChannels reporterChannels;
    public static final ArrayList<Integer> blacklist;
    private static boolean testing;
    private final String regionHelp = "Accepted channels are:\n\nall\nwodenweston = woden-weston = woden-weston-region = woden-weston-supporter\ngungahlin = gungahlin-region = gungahlin-supporter\ninnernorth = inner-north = inner-north-region = inner-north-supporter\nbelconnen = belconnen-region = belconnen-supporter\ninnersouth = inner-south = inner-south-region = inner-south-supporter\ntuggeranong = tuggeranong-region = tuggeranong-supporter\nqueanbeyan = queanbeyan-region = queanbeyan-supporter\nlegacy = legacyrare = legacy-rare = legacy-rare-supporter\nlarvitar = larvitarcandy = larvitar-candy = larvitar-candy-supporter\ndratini = dratinicandy = dratini-candy = dratini-candy-supporter\nmareep = mareepcandy = mareep-candy = mareep-candy-supporter\nultrarare = ultra-rare = ultra-rare-supporter\n100iv = 100-iv = 100% = 100-iv-supporter\nsnorlax = snorlax-supporter\nevent\n0iv = 0-iv = 0% = 0-iv-supporter\ndexfiller = dex-filler\nbigfishlittlerat = big-fish-little-rat = big-fish-little-rat-cardboard-box\n";
    private final String inputFormat = "```!addpokemon <pokemon1, pokemon2, pokemon3> <channel1, channel2, channel3>```\nFor as many pokemon and channels as you want. Make sure you include the <>. For more information on regions use the !channellis command";
    private final String nestHelp = "My nest commands are: \n```!nest <pokemon list> <status list>\n!nest pokemon status\n!nest pokemon\n!reportnest [your text here]\n!confirmed\n!suspected\n!fb or !nestfb\n!map or !nestmap\n!help\n```";
    private final String helpStr = "My commands are: \n```!addpokemon <pokemon list> <miniv,maxiv> <location list>\n!addpokemon pokemon\n!delpokemon <pokemon list> <miniv,maxiv> <location list>\n!delpokemon pokemon\n!clearpokemon <pokemon list>\n!clearlocation <location list>\n!reset\n!settings\n!help\n!channellist or !channels```";
    private static MessageChannel arrivalLounge;

    public static void main(final String[] args) {
        System.out.println("Connecting to db");
        if (MessageListener.testing) {
            DBManager.novabotdbConnect("root", "mimi");
            DBManager.rocketmapdbConnect("root", "mimi");
        }
        else {
            DBManager.rocketmapdbConnect("novabot", "Password123");
            DBManager.novabotdbConnect("novabot", "Password123");
        }
        System.out.println("Connected");
        try {
            final JDA jda = new JDABuilder(AccountType.BOT).setAutoReconnect(true).setGame(Game.of("Pokemon Go")).setToken(MessageListener.testing ? "MjkzMzI4Mjc0MDk0ODE3Mjgw.C7I4JQ.GFPR4D0KhFNiae53NtZj1_xpv0g" : "MjkyODI5NTQzODM0NTgzMDQw.C7KPXw.-_xS4cfCczUAH7AkloDbFf3uGgc").addListener(new MessageListener()).buildBlocking();
            final Iterator<Guild> iterator = jda.getGuilds().iterator();
            while (iterator.hasNext()) {
                final Guild guild = MessageListener.cbrSightings = iterator.next();
                System.out.println(guild.getName());
                if (guild.getTextChannelsByName(MessageListener.testing ? "novabot-testing" : "novabot", true).size() > 0) {
                    final TextChannel channel = guild.getTextChannelsByName(MessageListener.testing ? "novabot-testing" : "novabot", true).get(0);
                    System.out.println(channel.getName());
                    channel.sendMessage("Hi! I'm awake again!").queue();
                }
            }
            MessageListener.trello = MessageListener.cbrSightings.getTextChannelsByName("updates", true).get(0);
            MessageListener.arrivalLounge = MessageListener.cbrSightings.getTextChannelsByName("mod-log", true).get(0);
            MessageListener.nestsReports = MessageListener.cbrSightings.getTextChannelsByName("nests-reports", true).get(0);
            if (!MessageListener.testing) {
                final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(new Notifier(jda), 0L, 2L, TimeUnit.SECONDS);
            }
        }
        catch (LoginException | InterruptedException | RateLimitedException ex2) {
            ex2.printStackTrace();
        }
        System.out.println("connected");
    }

    @Override
    public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
        final User user = event.getMember().getUser();
        String roleStr = "";
        for (final Role role : event.getRoles()) {
            roleStr = roleStr + role.getName() + " ";
        }
        MessageListener.trello.sendMessage(user.getAsMention() + " had " + roleStr + "role(s) added").queue();
    }

    @Override
    public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
        final User user = event.getMember().getUser();
        String roleStr = "";
        for (final Role role : event.getRoles()) {
            roleStr = roleStr + role.getName() + " ";
        }
        MessageListener.trello.sendMessage(user.getAsMention() + " had " + roleStr + "role(s) removed").queue();
    }

    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        final JDA jda = event.getJDA();
        final User user = event.getMember().getUser();
        MessageListener.arrivalLounge.sendMessage(user.getAsMention() + " joined.").queue();
        DBManager.logNewUser(user.getId());
    }

    @Override
    public void onGuildMemberNickChange(final GuildMemberNickChangeEvent event) {
        final JDA jda = event.getJDA();
        final User user = event.getMember().getUser();
        MessageListener.arrivalLounge.sendMessage(user.getAsMention() + " has changed their nickname from " + event.getPrevNick() + " to " + event.getNewNick()).queue();
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        final JDA jda = event.getJDA();
        final long responseNumber = event.getResponseNumber();
        final User author = event.getAuthor();
        final Message message = event.getMessage();
        final MessageChannel channel = event.getChannel();
        final String msg = message.getContent();
        final boolean bot = author.isBot();
        if (author.getId().equals(jda.getSelfUser().getId())) {
            System.out.printf("<%s>: %s\n", author.getName(), msg);
            return;
        }
        if (event.isFromType(ChannelType.TEXT)) {
            final Guild guild = event.getGuild();
            final TextChannel textChannel = event.getTextChannel();
            MessageListener.cbrSightings = guild;
            final Member member = event.getMember();
            final String name = (member == null) ? "WEBHOOK" : member.getEffectiveName();
            if (channel.getName().equals(MessageListener.testing ? "novabot-testing" : "novabot")) {
                this.parseMsg(msg.toLowerCase(), author, textChannel);
            }
            else if (channel.getName().equals(MessageListener.testing ? "nests-testing" : "nests")) {
                this.parseNestMsg(msg.toLowerCase().trim(), author, channel, event.getChannelType());
            }
            else {
                if (MessageListener.testing) {
                    if (message.isWebhookMessage()) {
                        return;
                    }
                }
                else if (!message.isWebhookMessage()) {
                    return;
                }
                if (MessageListener.reporterChannels.containsChannel(channel.getName())) {
                    final Region channelRegion = ReporterChannels.getRegionByName(channel.getName());
                    System.out.println("Converted channel name to region: " + channelRegion);
                    final String msgTitle = message.getEmbeds().get(0).getTitle();
                    final int suburbStart = msgTitle.indexOf("[") + 1;
                    final int suburbEnd = msgTitle.indexOf("]");
                    final String suburb = msgTitle.substring(suburbStart, suburbEnd);
                    final int pokeStart = suburbEnd + 2;
                    final int pokeEnd = msgTitle.substring(pokeStart).indexOf(" ") + pokeStart;
                    final String pokeName = msgTitle.substring(pokeStart, pokeEnd).toLowerCase().trim();
                    String form = null;
                    if (pokeName.equals("Unown")) {
                        final int formStart = msgTitle.substring(pokeEnd).indexOf("[") + pokeEnd;
                        form = msgTitle.substring(formStart, formStart + 1);
                    }
                    final int ivStart = pokeEnd + 1;
                    final int ivEnd = msgTitle.indexOf("%");
                    final float pokeIV = Float.parseFloat(msgTitle.substring(ivStart, ivEnd));
                    final String msgBody = message.getEmbeds().get(0).getDescription();
                    final int moveSetStart = msgBody.indexOf("Moveset: ");
                    final int moveSetEnd = channel.getName().endsWith("-supporter") ? msgBody.indexOf("Gender: ") : msgBody.length();
                    final int moveSetSplit = msgBody.substring(moveSetStart, moveSetEnd).indexOf("-") + moveSetStart;
                    final String move_1 = msgBody.substring(moveSetStart, moveSetSplit).trim().toLowerCase();
                    final String move_2 = msgBody.substring(moveSetSplit + 2, moveSetEnd).trim().toLowerCase();
                    final PokeSpawn pokeSpawn = new PokeSpawn(Pokemon.nameToID(pokeName), suburb, pokeIV, move_1, move_2, form);
                    System.out.println(pokeSpawn.toString());
                    final ArrayList<String> ids = DBManager.getUserIDsToNotify(pokeSpawn);
                    if (channelRegion == Region.Event) {
                        this.alertPublic(message, ids);
                    }
                    else if (!channel.getName().endsWith("-supporter")) {
                        System.out.println("alerting non-supporters");
                        this.alertPublic(message, ids);
                    }
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            final PrivateChannel privateChannel = event.getPrivateChannel();
            System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
            if (msg.equals("!nesthelp")) {
                channel.sendMessage("My nest commands are: \n```!nest <pokemon list> <status list>\n!nest pokemon status\n!nest pokemon\n!reportnest [your text here]\n!confirmed\n!suspected\n!fb or !nestfb\n!map or !nestmap\n!help\n```").queue();
                return;
            }
            if (!msg.startsWith("!nest") && !msg.startsWith("!map")) {
                if (!msg.startsWith("fb")) {
                    this.parseMsg(msg.toLowerCase(), author, privateChannel);
                }
            }
        }
        else if (event.isFromType(ChannelType.GROUP)) {
            final Group group = event.getGroup();
            final String groupName = (group.getName() != null) ? group.getName() : "";
            System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
        }
    }

    private void parseNestMsg(final String msg, final User author, final MessageChannel channel, final ChannelType channelType) {
        if (!msg.startsWith("!")) {
            return;
        }
        switch (msg) {
            case "!fb":
            case "!nestfb": {
                channel.sendMessage("https://www.facebook.com/groups/PogoCBRNests/").queue();
                break;
            }
            case "!map":
            case "!nestmap": {
                channel.sendMessage("http://www.google.com/maps/d/u/0/viewer?mid=1d-QuaDK1tJRiHKODXErTQIDqIAY").queue();
                break;
            }
            case "!help": {
                channel.sendMessage("My nest commands are: \n```!nest <pokemon list> <status list>\n!nest pokemon status\n!nest pokemon\n!reportnest [your text here]\n!confirmed\n!suspected\n!fb or !nestfb\n!map or !nestmap\n!help\n```").queue();
                break;
            }
            default: {
                if (msg.startsWith("!reportnest")) {
                    final String report = msg.substring(msg.indexOf(" ") + 1);
                    MessageListener.nestsReports.sendMessage(author.getAsMention() + " reported: \"" + report + "\"").queue();
                    return;
                }
                if (msg.startsWith("!confirmed")) {
                    final MessageBuilder builder = new MessageBuilder();
                    builder.append((CharSequence)author.getAsMention()).append((CharSequence)"");
                    final ArrayList<Nest> nests = NestSheetManager.getNestsByStatus(new NestStatus[] { NestStatus.Confirmed });
                    if (nests.size() == 0) {
                        builder.append((CharSequence)" sorry, I couldn't find any confirmed nests");
                        channel.sendMessage(builder.build()).queue();
                    }
                    else {
                        if (channelType != ChannelType.PRIVATE) {
                            channel.sendMessage(author.getAsMention() + " I have PM'd you your search results").queue();
                        }
                        builder.append((CharSequence)" I found ").append(nests.size()).append((CharSequence)" confirmed nests:\n\n");
                        for (final Nest nest : nests) {
                            builder.append(nest).append((CharSequence)"\n");
                            builder.append((CharSequence)"<").append((CharSequence)nest.getGMapsLink()).append((CharSequence)">\n\n");
                        }
                        if (!author.hasPrivateChannel()) {
                            author.openPrivateChannel().complete();
                        }
                        for (final Message message : builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE)) {
                            author.getPrivateChannel().sendMessage(message).queue();
                        }
                    }
                    return;
                }
                if (msg.equals("!suspected")) {
                    final MessageBuilder builder = new MessageBuilder();
                    builder.append((CharSequence)author.getAsMention()).append((CharSequence)"");
                    final ArrayList<Nest> nests = NestSheetManager.getNestsByStatus(new NestStatus[] { NestStatus.Suspected });
                    if (nests.size() == 0) {
                        builder.append((CharSequence)" sorry, I couldn't find any suspected nests");
                        channel.sendMessage(builder.build()).queue();
                    }
                    else {
                        if (channelType != ChannelType.PRIVATE) {
                            channel.sendMessage(author.getAsMention() + " I have PM'd you your search results").queue();
                        }
                        builder.append((CharSequence)" I found ").append(nests.size()).append((CharSequence)" suspected nests:\n\n");
                        for (final Nest nest : nests) {
                            builder.append(nest).append((CharSequence)"\n");
                            builder.append((CharSequence)"<").append((CharSequence)nest.getGMapsLink()).append((CharSequence)">\n\n");
                        }
                        if (!author.hasPrivateChannel()) {
                            author.openPrivateChannel().complete();
                        }
                        for (final Message message : builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE)) {
                            author.getPrivateChannel().sendMessage(message).queue();
                        }
                    }
                    return;
                }
                final UserCommand userCommand = Parser.parseInput(msg);
                final ArrayList<InputError> exceptions = userCommand.getExceptions();
                final String cmdStr = (String)userCommand.getArg(0).getParams()[0];
                if (exceptions.size() > 0) {
                    String errorMessage = author.getAsMention() + ", I had " + ((exceptions.size() == 1) ? "a problem" : "problems") + " reading your input.\n\n";
                    final InputError error = InputError.mostSevere(exceptions);
                    errorMessage += error.getErrorMessage(userCommand);
                    channel.sendMessage(errorMessage).queue();
                    break;
                }
                if (!cmdStr.startsWith("!nest")) {
                    break;
                }
                final NestSearch nestSearch = userCommand.buildNestSearch();
                final MessageBuilder builder2 = new MessageBuilder();
                builder2.append((CharSequence)author.getAsMention()).append((CharSequence)"");
                final ArrayList<Nest> nests2 = NestSheetManager.getNests(nestSearch);
                if (nests2.size() == 0) {
                    builder2.append((CharSequence)" sorry, I couldn't find any ").append((CharSequence)NestStatus.listToString(nestSearch.getStatuses())).append((CharSequence)" nests for ").append((CharSequence)Pokemon.listToString(nestSearch.getPokemon()));
                    channel.sendMessage(builder2.build()).queue();
                    break;
                }
                if (channelType != ChannelType.PRIVATE) {
                    channel.sendMessage(author.getAsMention() + " I have PM'd you your search results").queue();
                }
                builder2.append((CharSequence)" I found ").append(nests2.size()).append((CharSequence)" results for ").append((CharSequence)NestStatus.listToString(nestSearch.getStatuses())).append((CharSequence)" ").append((CharSequence)Pokemon.listToString(nestSearch.getPokemon())).append((CharSequence)" nests :\n\n");
                for (final Pokemon poke : nestSearch.getPokemon()) {
                    builder2.append((CharSequence)"**").append((CharSequence)poke.name).append((CharSequence)"**\n");
                    boolean foundPoke = false;
                    for (final Nest nest2 : nests2) {
                        if (nest2.pokemon.name.equals(poke.name)) {
                            foundPoke = true;
                            builder2.append((CharSequence)"  ").append(nest2).append((CharSequence)"\n");
                            builder2.append((CharSequence)"  <").append((CharSequence)nest2.getGMapsLink()).append((CharSequence)">\n\n");
                        }
                    }
                    if (!foundPoke) {
                        builder2.append((CharSequence)"  No results found\n");
                    }
                }
                if (!author.hasPrivateChannel()) {
                    author.openPrivateChannel().complete();
                }
                for (final Message message2 : builder2.buildAll(MessageBuilder.SplitPolicy.NEWLINE)) {
                    author.getPrivateChannel().sendMessage(message2).queue();
                }
                break;
            }
        }
    }

    private void alertPublic(final Message message, final ArrayList<String> userIDs) {
        for (final String userID : userIDs) {
            if (isSupporter(userID)) {
                continue;
            }
            if (DBManager.countPokemon(userID) > 3) {
                final User user = MessageListener.cbrSightings.getMemberById(userID).getUser();
                if (!user.hasPrivateChannel()) {
                    user.openPrivateChannel().complete();
                }
                user.getPrivateChannel().sendMessage("Hi " + user.getAsMention() + ", I noticed recently you have lost your supporter status. As a result I have cleared your settings, however as a non-supporter you can add up to 3 pokemon to your settings").queue();
                DBManager.resetUser(userID);
                MessageListener.trello.sendMessage(user.getAsMention() + " has lost supporter status with more than 3 pokemon in their settings. Their settings have been reset and they have been PMed").queue();
            }
            else {
                final MessageBuilder builder = new MessageBuilder();
                builder.setEmbed(message.getEmbeds().get(0));
                System.out.println("Notifying user: " + userID);
                final User user2 = MessageListener.cbrSightings.getMemberById(userID).getUser();
                if (!user2.hasPrivateChannel()) {
                    user2.openPrivateChannel().complete();
                }
                user2.getPrivateChannel().sendMessage(builder.build()).queue();
            }
        }
    }

    public static boolean isSupporter(final String userID) {
        final Member member = MessageListener.cbrSightings.getMemberById(userID);
        for (final Role role : member.getRoles()) {
            if (role.getName().toLowerCase().contains("supporter")) {
                return true;
            }
        }
        return false;
    }

    private void parseMsg(final String msg, final User author, final MessageChannel channel) {
        if (!msg.startsWith("!")) {
            return;
        }
        if (msg.startsWith("!nest")) {
            channel.sendMessage(author.getAsMention() + " I only accept nest commands in the " + MessageListener.cbrSightings.getTextChannelsByName("nests", true).get(0).getAsMention() + " channel or via PM").queue();
            return;
        }
        if (msg.equals("!settings")) {
            final UserPref userPref = DBManager.getUserPref(author.getId());
            System.out.println("!settings");
            if (userPref == null || userPref.isEmpty()) {
                channel.sendMessage(author.getAsMention() + ", you don't have any notifications set. Add some with the !addpokemon command.").queue();
            }
            else {
                String toSend = author.getAsMention() + ", you are currently set to receive notifications for:\n\n";
                toSend += userPref.allPokemonToString();
                final MessageBuilder builder = new MessageBuilder();
                builder.append((CharSequence)toSend);
                final Queue<Message> messages = builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                for (final Message message : messages) {
                    channel.sendMessage(message).queue();
                }
            }
        }
        else if (msg.equals("!reset")) {
            DBManager.resetUser(author.getId());
            channel.sendMessage(author.getAsMention() + ", your notification settings have been reset").queue();
        }
        else if (msg.equals("!help")) {
            channel.sendMessage("My commands are: \n```!addpokemon <pokemon list> <miniv,maxiv> <location list>\n!addpokemon pokemon\n!delpokemon <pokemon list> <miniv,maxiv> <location list>\n!delpokemon pokemon\n!clearpokemon <pokemon list>\n!clearlocation <location list>\n!reset\n!settings\n!help\n!channellist or !channels```").queue();
        }
        else if (msg.equals("!channellist") || msg.equals("!channels")) {
            channel.sendMessage("Accepted channels are:\n\nall\nwodenweston = woden-weston = woden-weston-region = woden-weston-supporter\ngungahlin = gungahlin-region = gungahlin-supporter\ninnernorth = inner-north = inner-north-region = inner-north-supporter\nbelconnen = belconnen-region = belconnen-supporter\ninnersouth = inner-south = inner-south-region = inner-south-supporter\ntuggeranong = tuggeranong-region = tuggeranong-supporter\nqueanbeyan = queanbeyan-region = queanbeyan-supporter\nlegacy = legacyrare = legacy-rare = legacy-rare-supporter\nlarvitar = larvitarcandy = larvitar-candy = larvitar-candy-supporter\ndratini = dratinicandy = dratini-candy = dratini-candy-supporter\nmareep = mareepcandy = mareep-candy = mareep-candy-supporter\nultrarare = ultra-rare = ultra-rare-supporter\n100iv = 100-iv = 100% = 100-iv-supporter\nsnorlax = snorlax-supporter\nevent\n0iv = 0-iv = 0% = 0-iv-supporter\ndexfiller = dex-filler\nbigfishlittlerat = big-fish-little-rat = big-fish-little-rat-cardboard-box\n").queue();
        }
        else if (msg.startsWith("!")) {
            final UserCommand userCommand = Parser.parseInput(msg);
            final ArrayList<InputError> exceptions = userCommand.getExceptions();
            if (exceptions.size() > 0) {
                String errorMessage = author.getAsMention() + ", I had " + ((exceptions.size() == 1) ? "a problem" : "problems") + " reading your input.\n\n";
                final InputError error = InputError.mostSevere(exceptions);
                errorMessage += error.getErrorMessage(userCommand);
                channel.sendMessage(errorMessage).queue();
            }
            else {
                final String cmdStr = (String)userCommand.getArg(0).getParams()[0];
                if (cmdStr.contains("pokemon")) {
                    final Pokemon[] pokemons = userCommand.buildPokemon();
                    if (cmdStr.startsWith("!add")) {
                        if (!isSupporter(author.getId()) && DBManager.countPokemon(author.getId()) + pokemons.length > 3) {
                            channel.sendMessage(author.getAsMention() + " as a non-supporter, you may have a maximum of 3 pokemon notifications set up. What you tried to add would put you over this limit, please remove some pokemon with the !delpokemon command or try adding fewer pokemon.").queue();
                            return;
                        }
                        if (!DBManager.containsUser(author.getId())) {
                            DBManager.addUser(author.getId());
                        }
                        for (final Pokemon pokemon : pokemons) {
                            System.out.println("adding pokemon " + pokemon);
                            DBManager.addPokemon(author.getId(), pokemon);
                        }
                        String message2 = author.getAsMention() + " you will now be notified of " + Pokemon.listToString(userCommand.getUniquePokemon());
                        if (userCommand.containsArg(ArgType.Iv)) {
                            channel.sendMessage(author.getAsMention() + " please note my IV filtered notifications are inaccurate since Niantic's update. See " + MessageListener.cbrSightings.getTextChannelsByName("announcements", true).get(0).getAsMention() + " for more details").queue();
                            final Argument ivArg = userCommand.getArg(ArgType.Iv);
                            if (ivArg.getParams().length == 1) {
                                message2 = message2 + " " + ivArg.getParams()[0] + "% IV or above";
                            }
                            else {
                                message2 = message2 + " between " + ivArg.getParams()[0] + " and " + ivArg.getParams()[1] + "% IV";
                            }
                        }
                        final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                        Location[] locations = { new Location(Region.All) };
                        if (locationsArg != null) {
                            locations = userCommand.getLocations();
                        }
                        message2 = message2 + " in " + Location.listToString(locations);
                        channel.sendMessage(message2).queue();
                    }
                    else if (cmdStr.startsWith("!del")) {
                        for (final Pokemon pokemon : pokemons) {
                            DBManager.deletePokemon(author.getId(), pokemon);
                        }
                        String message2 = author.getAsMention() + " you will no longer be notified of " + Pokemon.listToString(userCommand.getUniquePokemon());
                        if (userCommand.containsArg(ArgType.Iv)) {
                            final Argument ivArg = userCommand.getArg(ArgType.Iv);
                            if (ivArg.getParams().length == 1) {
                                message2 = message2 + " " + ivArg.getParams()[0] + "% IV or above";
                            }
                            else {
                                message2 = message2 + " between " + ivArg.getParams()[0] + " and " + ivArg.getParams()[1] + "% IV";
                            }
                        }
                        final Argument locationsArg = userCommand.getArg(ArgType.Locations);
                        Location[] locations = { new Location(Region.All) };
                        if (locationsArg != null) {
                            locations = userCommand.getLocations();
                        }
                        message2 = message2 + " in " + Location.listToString(locations);
                        channel.sendMessage(message2).queue();
                    }
                    else if (cmdStr.startsWith("!clear")) {
                        DBManager.clearPokemon(author.getId(), new ArrayList<Pokemon>(Arrays.asList(pokemons)));
                        final String message2 = author.getAsMention() + " you will no longer be notified of " + Pokemon.listToString(pokemons) + " in any channels";
                        channel.sendMessage(message2).queue();
                    }
                }
                else if (cmdStr.equals("!clearlocation")) {
                    final Location[] locations2 = userCommand.getLocations();
                    DBManager.clearLocations(author.getId(), locations2);
                    final String message2 = author.getAsMention() + " you will no longer be notified of any pokemon in " + Location.listToString(locations2);
                    channel.sendMessage(message2).queue();
                }
            }
        }
    }

    static {
        reporterChannels = new ReporterChannels();
        blacklist = new ArrayList<Integer>(Arrays.asList(13, 16, 19, 21, 23, 29, 32, 41, 48, 60, 98, 115, 118, 120, 183, 161, 165, 167, 177, 194, 198));
        MessageListener.testing = false;
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Innernorth, "inner-north"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Innersouth, "inner-south"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.GungahlinRegion, "gungahlin"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.BelconnenRegion, "belconnen"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.TuggeranongRegion, "tuggeranong"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Wodenweston, "woden-weston"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.QueanbeyanRegion, "queanbeyan"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Legacyrare, "legacy-rare"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Ultrarare, "ultra-rare"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Hundrediv, "100-iv"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.DratiniCandy, "dratini-candy"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.LarvitarCandy, "larvitar-candy"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.MareepCandy, "mareep-candy"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.SnorlaxCandy, "snorlax"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Event, "event"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Zeroiv, "0-iv"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.Hundrediv, "dex-filler"));
        MessageListener.reporterChannels.add(new ReporterChannel(Region.BigFishLittleRat, "big-fish-little-rat"));
    }
}