package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.*;
import java.util.stream.Collectors;

public class LabelNavigator extends BotScenario {
    private static final String EMOJI_CROSS = "\u274c", EMOJI_CHECK = "\u2705";

    private LabelRepository labelRepository;
    private DataRepository dataRepository;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();

        if (request.update.hasCallbackQuery()) {
            Message oldContentsMessage = request.update.getCallbackQuery().getMessage();
            String callback = request.update.getCallbackQuery().getData();

            Label callbackLabel = labelRepository.findByNameAndClientId(callback, client.getId()
            ).orElseThrow(() -> new IllegalStateException("Label '" + callback + "' does not exist"));

            if (getState().has(callback) || isLabelInPath(callback)) {
                if (getState().has(callback)) {
                    getState().remove(callback);
                    setState(getState());
                }

                EditMessageText editMessageText = botUtils.plainEdit(
                        oldContentsMessage,
                        "Here is a list of data, marked by '" + callback + "'" +
                                oldContentsMessage.getText().substring(oldContentsMessage.getText().indexOf("\n"))
                ).setReplyMarkup(sublabelsMarkup(callbackLabel));

                try {
                    bot.execute(editMessageText);
                } catch (TelegramApiRequestException e) {
                    e.printStackTrace();
                    System.err.println(e.getApiResponse());
                } catch (TelegramApiException e) {
                    logger.error("Failed to check sublabels of label info in request " + request, e);
                }
            } else {
                Label parent = labelRepository.findByNameAndClientId(
                        getCurrentDir(), client.getId()
                ).orElseThrow(() -> new IllegalStateException("Label '" + getCurrentDir() + "' does not exist"));

                setState(getState().put(callback, "checked"));
                try {
                    bot.execute(new EditMessageReplyMarkup()
                            .setChatId(oldContentsMessage.getChatId())
                            .setMessageId(oldContentsMessage.getMessageId())
                            .setReplyMarkup(sublabelsMarkup(parent)));
                } catch (TelegramApiException e) {
                    logger.error("Failed to check label info in request " + request, e);
                }
            }
            return false;
        }

        logger.info("LabelNavigator - Starting for " + client);

        try {
            Optional<Label> allDataOp = labelRepository.findByNameAndClientId("all data", client.getId());
            Label allData = allDataOp.orElseThrow(() -> new IllegalStateException(String.format(
                    "No 'all data' label found for user %s, request %s",
                    client, request)));

            SendMessage sendLabelInfo = botUtils.markdownMessage(
                    "Search for labels and data. Send me text (without line breaks and its length should " +
                            "be less than 50 characters) to save it as a label. Send me a data URL to assign " +
                            "your current label to it",
                    botUtils.retrieveChat(request.update).getId()
            );
            sendLabelInfo.setReplyMarkup(new ReplyKeyboardMarkup().setKeyboard(
                    Arrays.asList(
                            new KeyboardRow() {{
                                add(new KeyboardButton("Send data by selected label(s)"));
                            }},
                            new KeyboardRow() {{
                                add(new KeyboardButton("Rename label"));
                            }},
                            new KeyboardRow() {{
                                add(new KeyboardButton("Delete label"));
                            }},
                            new KeyboardRow() {{
                                add(new KeyboardButton("To main menu"));
                            }}
                    )).setResizeKeyboard(true)
            );
            bot.execute(sendLabelInfo);
//        ---------------------------------------------------------------------------

            StringBuilder dataContent =
                    new StringBuilder("Here is a list of data, marked by '" + allData.getName() + "'\n");

            Set<String> labelNamesRecursive = allData.getAllSonsRecursively().stream()
                    .map(Label::getName)
                    .collect(Collectors.toSet());

            int lengthOld = dataContent.length();
            dataRepository.findAllByLabelNamesAndClientId(labelNamesRecursive, client.getId()).forEach(
                    data -> dataContent.append("\n")
                            .append(data.getUrl()).append(" - ").append(data.getDescription()));
            if (dataContent.length() == lengthOld) {
                dataContent.append("\n").append("Well, no data yet");
            }

            SendMessage sendLabelContents = botUtils.plainMessage(
                    dataContent.toString(), botUtils.retrieveChat(request.update).getId()
            );

            setState(getState().put("PATH", ""));
            sendLabelContents.setReplyMarkup(sublabelsMarkup(allData));
            bot.execute(sendLabelContents);

            return false;
        } catch (Exception e) {
            logger.error("Failed to process LabelNavigator in update " + request.update, e);
        }

        setStage(null);
        logger.info("LabelNavigator - Finished for " + client);
        return true;
    }

    private InlineKeyboardMarkup sublabelsMarkup(Label label) {
        String currentPath = getState().get("PATH").toString();

        if (isLabelInPath(label.getName())) {
            setState(getState().put("PATH",
                    currentPath.substring(0, currentPath.indexOf(label.getName()) + label.getName().length())));
        } else {
            setState(getState().put("PATH", currentPath + "/" + label.getName()));
        }

     // TODO: 16.02.19 Capitalize reserved names, restrict using / in names, make users' labels lowerCased

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (null != label.getParents() && !label.getParents().isEmpty()) {
            InlineKeyboardButton levelUp = new InlineKeyboardButton("Go back to '" + getLevelUpDir() + "'");
            levelUp.setCallbackData(getLevelUpDir());
            buttons.add(Collections.singletonList(levelUp));
        }

        int labelsInRow = label.getSons().size() > 4 ? 3 : 2;
        List<Label> sons = new ArrayList<>(label.getSons());

        for (int i = 0; i < sons.size() / labelsInRow + 1; i++) {
            List<InlineKeyboardButton> row = new LinkedList<>();
            for (int j = 0; j < labelsInRow && i * labelsInRow + j < sons.size(); j++) {
                Label son = sons.get(i * labelsInRow + j);
                String emoji = getState().has(son.getName()) ? EMOJI_CHECK : EMOJI_CROSS;
                InlineKeyboardButton labelButton = new InlineKeyboardButton(emoji + son.getName());
                labelButton.setCallbackData(son.getName());
                row.add(labelButton);
            }
            if (!row.isEmpty())
                buttons.add(row);
        }
        return new InlineKeyboardMarkup().setKeyboard(buttons);
    }

    private boolean isLabelInPath(String label) {
        String currentPath = getState().get("PATH").toString();
        List<String> hierarchy = Arrays.stream(currentPath.split("/")).collect(Collectors.toList());

        return hierarchy.contains(label);
    }

    private String getCurrentDir() {
        String currentPath = getState().get("PATH").toString();
        String[] hierarchy = currentPath.split("/");
        if (hierarchy.length > 0) {
            return hierarchy[hierarchy.length - 1];
        } else {
            return "NONE";
        }
    }

    private String getLevelUpDir() {
        String currentPath = getState().get("PATH").toString();
        String[] hierarchy = currentPath.split("/");
        if (hierarchy.length > 1) {
            return hierarchy[hierarchy.length - 2];
        } else {
            return "NONE";
        }
    }

    public LabelNavigator(ScenarioState state) {
        super(state);
    }

    @Autowired
    public void setLabelRepository(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Autowired
    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(LabelNavigator.class);
}