package haveric.recipeManager.flags;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

public class FlagCommand extends Flag {

    @Override
    protected String[] getArguments() {
        return new String[] {
            "{flag} <text or false>", };
    }

    @Override
    protected String[] getDescription() {
        return new String[] {
            "Executes the command when recipe is successful.",
            "This flag can be used more than once to add more commands to the list.",
            "",
            "Commands are executed server-side, if you add / prefix it will execute the command on the crafter.",
            "",
            "You can also use these variables:",
            "  {player}         = crafter's name or '(nobody)' if not available",
            "  {playerdisplay}  = crafter's display name or '(nobody)' if not available",
            "  {result}         = the result item name or '(nothing)' if recipe failed.",
            "  {recipename}     = recipe's custom or autogenerated name or '(unknown)' if not available",
            "  {recipetype}     = recipe type or '(unknown)' if not available",
            "  {inventorytype}  = inventory type or '(unknown)' if not available",
            "  {world}          = world name of event location or '(unknown)' if not available",
            "  {x}              = event location's X coord or '(?)' if not available",
            "  {y}              = event location's Y coord or '(?)' if not available",
            "  {z}              = event location's Z coord or '(?)' if not available", };
    }

    @Override
    protected String[] getExamples() {
        return new String[] {
            "{flag} /say I crafted {result}!",
            "{flag} kick {player}", };
    }


    private List<String> commands = new ArrayList<String>();

    public FlagCommand() {
    }

    public FlagCommand(FlagCommand flag) {
        commands.addAll(flag.commands);
    }

    @Override
    public FlagCommand clone() {
        super.clone();
        return new FlagCommand(this);
    }

    /**
     * @return the command list
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Set the command list.<br>
     * You can use null to remove the entire flag.
     *
     * @param commands
     */
    public void setCommands(List<String> newCommands) {
        if (newCommands == null) {
            remove();
        } else {
            commands = newCommands;
        }
    }

    /**
     * Adds a command to the list.<br>
     * You can use null, "false" or "remove" to remove the entire flag.
     *
     * @param command
     */
    public void addCommand(String command) {
        if (command == null || command.equalsIgnoreCase("false") || command.equalsIgnoreCase("remove")) {
            remove();
        } else {
            commands.add(command);
        }
    }

    @Override
    protected boolean onParse(String value) {
        addCommand(value);

        return true;
    }

    @Override
    protected void onCrafted(Args a) {
        for (String command : commands) {
            if (command.charAt(0) == '/') {
                if (!a.hasPlayer()) {
                    a.addCustomReason("Need player!");
                    return;
                }

                break;
            }
        }

        for (String command : commands) {
            command = a.parseVariables(command);

            if (command.charAt(0) == '/') {
                a.player().chat(command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    /*
     * @Override public List<String> information() { if(commands.isEmpty()) { return null; }
     *
     * List<String> list = new ArrayList<String>(commands.size());
     *
     * for(String command : commands) { if(command.charAt(0) == '/') { list.add(Messages.FLAG_COMMAND_PLAYER.get("{command}", command)); } else { list.add(Messages.FLAG_COMMAND_SERVER.get("{command}",
     * command)); } }
     *
     * return list; }
     */
}
