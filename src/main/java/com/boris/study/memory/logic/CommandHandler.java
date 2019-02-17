package com.boris.study.memory.logic;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.data.DataShower;
import com.boris.study.memory.logic.helpers.HelpShower;
import com.boris.study.memory.logic.label.LabelNavigator;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler extends BotScenario {
    private static final String SEARCH_COMMAND = "/search", LABELS_COMMAND = "/label", HELP_COMMAND = "/help";

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();
        int stageHandled = 0;

        if (null == getStage() || getStage() < stageHandled) {
            logger.info("CommandHandler - Starting for " + client);
            try {
                List<String> commands = retrieveCommands(request.update.getMessage().getText());
                if (commands.size() == 0) {
                    throw new IllegalArgumentException("An update without commands passed to CommandHandler");
                } else if (commands.size() > 1) {
                    return processOther(DataSaver.class, request);
                }

                String command = commands.get(0).trim();
                setStage(stageHandled);

                if ("/start".equals(command)) {

                    bot.execute(botUtils.markdownMessage(
                            uiUtils.getErrors().getNeedlessStart(),
                            botUtils.retrieveChat(request.update).getId()
                    ));
                } else if (HELP_COMMAND.equals(command)) {

                    processStateless(HelpShower.class, request);
                } else if (SEARCH_COMMAND.equals(command)) {

                    bot.execute(botUtils.markdownMessage(
                            "No data yet, nothing to search", botUtils.retrieveChat(request.update).getId()
                    ));
                } else if (LABELS_COMMAND.equals(command)) {

                    if (!processOther(LabelNavigator.class, request))
                        return false;
                } else if (dataUtils.isValidDataUrl(command)) {

                    boolean dataShowerFinished = processOther(DataShower.class, new Request(request.update) {{
                        put(DataShower.KEY_URL, command);
                    }});
                    if (!dataShowerFinished)
                        return false;
                } else {
                    bot.execute(botUtils.markdownMessage(
                            uiUtils.getErrors().getUnknownCommand(),
                            botUtils.retrieveChat(request.update).getId()
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to handle commands in request " + request, e);
            }
        }

        setStage(null);
        logger.info("CommandHandler - Finished for " + client);
        return true;
    }

    public CommandHandler(ScenarioState state) {
        super(state);
    }

    private static List<String> retrieveCommands(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(\\G|\\s|^)/\\w+($|\\s)").matcher(text);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    /**
     * Does not handle data links, they look like /d_someRandomDataUrl
     *
     * @return a List<String> of all known commands, that are not links to some data
     */
    public static List<String> getKnownCommands() {
        return Arrays.asList(SEARCH_COMMAND, LABELS_COMMAND, HELP_COMMAND);
    }

    private final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
}