package com.github.novskey.novabot.parser;

import java.util.ArrayList;
import java.util.HashSet;

public enum InputError
{
    InvalidArg,
    BlacklistedPokemon,
    InvalidArgCombination,
    DuplicateArgs,
    MissingRequiredArg,
    MalformedArg,
    InvalidCommand, ;

    public String getErrorMessage(final UserCommand userCommand) {
        switch (this) {
            case InvalidArg: {
                String                 str      = "Dieser Befehl akzeptiert nur ";
                final HashSet<ArgType> argTypes = userCommand.novaBot.commands.get((String) userCommand.getArg(0).getParams()[0]).validArgTypes;
                str = str + ArgType.setToString(argTypes) + "";
                return str;
            }
            case BlacklistedPokemon: {
                StringBuilder str = new StringBuilder("Eines oder mehr Pokemon die du eingeben hast stehen auf der Blacklist\n\n");
                for (final String s : userCommand.getBlacklisted()) {
                    str.append("  ").append(s).append("\n");
                }
                return str.toString();
            }
            case InvalidArgCombination: {
                StringBuilder str = new StringBuilder("Du hast eine falsche Eingabenkombination für diesen Befehl getätigt. Siehe `!help ")
                        .append(((String)userCommand.getArg(ArgType.CommandStr).getParams()[0]).substring(1))
                        .append("` um alle möglichen Kombinationen zu sehen");
                return str.toString();
            }
            case DuplicateArgs: {
                final ArgType duplicateType = Argument.getDuplicateArg(userCommand.getArgs());
                String str = "Du hast mehrere angaben gemacht " + duplicateType;
                if (duplicateType == ArgType.Float || duplicateType == ArgType.Int || duplicateType == ArgType.IV) {
                    return str;
                }
                if (duplicateType == ArgType.CommandStr) {
                    return str + "s. Bitte gebe deine Befehle in verschiedenen Nachrichten ein.";
                }
                if (duplicateType == ArgType.Locations) {
                    str += "s";
                }

                str += " ohne sie in einer Liste zu haben.";
                str = str + " Wenn du mehrere eingeben möchtest " + duplicateType + ((duplicateType != ArgType.Pokemon) ? "s" : "") + ", bitte in einer Liste eingeben wie z. B:\n\n";
                assert duplicateType != null;
                switch (duplicateType) {
                    case Pokemon:
                        str += "`!addpokemon <dragoran,lapras,icognito>`";
                        break;
                    case Locations:
                        str += "`!addpokemon lapras <braunschweig,peine>`";
                        break;

                }
                return str + "\n\n";
            }
            case MissingRequiredArg: {
                final HashSet<ArgType> requiredArgs = userCommand.novaBot.commands.get((String) userCommand.getArg(0).getParams()[0]).getRequiredArgTypes();
                final String           str          = "Für diesen Befehl musst du eins oder mehrere angeben " + ArgType.setToString(requiredArgs);
                return str + "\n\n";
            }
            case MalformedArg: {
                return "Ich konnte den Befehl nicht deuten:\n\n" + Argument.malformedToString(userCommand.getMalformedArgs());
            }
            case InvalidCommand: {
                return "Ich haben den Befehl nicht erkannt, nutze `!help` für eine Liste meiner Befehle";
            }
            default:
                return null;
        }
    }

    public static InputError mostSevere(final ArrayList<InputError> exceptions) {
        InputError error = InputError.InvalidArg;
        for (final InputError exception : exceptions) {
            if (exception.ordinal() > error.ordinal()) {
                error = exception;
            }
        }
        return error;
    }

}
