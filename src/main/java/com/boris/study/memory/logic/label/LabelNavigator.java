package com.boris.study.memory.logic.label;

import com.boris.study.memory.data.entity.Client;
import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.entity.ScenarioState;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import com.boris.study.memory.logic.data.DataSaver;
import com.boris.study.memory.logic.data.LabelAssigner;
import com.boris.study.memory.logic.helpers.CommandsShower;
import com.boris.study.memory.logic.sructure.BotScenario;
import com.boris.study.memory.logic.sructure.Request;
import com.boris.study.memory.utils.DataUtils;
import com.boris.study.memory.utils.LabelUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;

public class LabelNavigator extends BotScenario {
    private static final String EMOJI_CROSS = "\u274c", EMOJI_CHECK = "\u2705", EMOJI_LEFT = "\u2b05";

    private LabelRepository labelRepository;
    private DataRepository dataRepository;
    private LabelUtils labelUtils;

    @Override
    public Boolean process(Request request, boolean forceRestart) {
        if (!continueProcessing(request, forceRestart))
            return false;

        Client client = getClient();

        int stageInitialized = 0;
        int stageDataSaved = 1;

        if (null == getStage() || getStage() < stageInitialized) {
            logger.info("LabelNavigator - Starting for " + client);
            init(request);
            setStage(stageInitialized);
            return false;
        }

        logger.info("LabelNavigator - Continuing for " + client);

        if (null != getStage() && getStage() == stageDataSaved) {
            String newDataUrl = request.get(DataSaver.Result.NEW_DATA_URL.name());
            String currentName = labelUtils.getCurrentDir(getPath());

            assignLabelToData(currentName, newDataUrl, request);
            updateNavigator(labelUtils.obtain(currentName, client.getId()), request);

            setStage(stageInitialized);
            return false;
        }

        if (request.update.hasCallbackQuery()) {
            Label callbackLabel = labelUtils.obtain(request.update.getCallbackQuery().getData(), client.getId());
            updateNavigator(callbackLabel, request);

        } else if (request.update.hasMessage()) {
            if (request.update.getMessage().hasText()) {

                String text = request.update.getMessage().getText();
                if (dataUtils.isValidDataUrl(text)) {
                    if (!dataRepository.existsById(text)) {
                        try {
                            bot.execute(botUtils.markdownMessage(
                                    uiUtils.getErrors().getNoDataByUrl(),
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException e) {
                            logger.error("Couldn't send error to client + " + client, e);
                        }
                    } else { // treated as a name of a label
                        Data data = dataRepository.findByUrl(text).get();
                        Label label = labelUtils.obtain(labelUtils.getCurrentDir(getPath()), client.getId());

                        if (data.getLabels().contains(label)) {
                            try {
                                bot.execute(botUtils.plainMessage("Label '" + label.getName() +
                                                "' is already assigned to this data",
                                        botUtils.retrieveChat(request.update).getId()));
                            } catch (TelegramApiException e) {
                                logger.error(
                                        String.format(
                                                "Couldn't tell client %s that label %s is already assigned to %s",
                                                client, label, data),
                                        e);
                            }
                        } else {
                            String currentName = labelUtils.getCurrentDir(getPath());
                            assignLabelToData(currentName, request.update.getMessage().getText(), request);
                            updateNavigator(labelUtils.obtain(currentName, client.getId()), request);
                        }
                    }
                } else if (text.equals("To main menu")) {
                    processStateless(CommandsShower.class, request);
                    setState(new JSONObject());
                    setStage(null);
                    logger.info("LabelNavigator - Finished for " + client);
                    return true;
                } else {
                    if (dataUtils.isValidLabelName(text)) {
                        String name = text.toLowerCase();
                        Label label = labelRepository.findByNameAndClientId(name, client.getId()).orElseGet(() -> {
                            try {
                                bot.execute(botUtils.plainMessage(
                                        "This label doesn't exist now, but I will create it",
                                        botUtils.retrieveChat(request.update).getId()));
                            } catch (TelegramApiException e) {
                                logger.error("Couldn't inform about creating new label in request " + request, e);
                            }

                            return new Label(name, client);
                        });

                        Label current = labelRepository
                                .findByNameAndClientId(
                                        labelUtils.getCurrentDir(getPath()),
                                        client.getId())
                                .orElseThrow(() -> new IllegalStateException("No current label '" +
                                        labelUtils.getCurrentDir(getPath()) +
                                        "' found for request " + request));

                        current.addSon(label);
                        labelRepository.save(label);
                        labelRepository.save(current);
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Successfully added a sublabel '" + name + "'",
                                    botUtils.retrieveChat(request.update).getId()));
                        } catch (TelegramApiException e) {
                            logger.error("Couldn't inform about a sublabel in request " + request, e);
                        }
                        updateNavigator(current, request);
                    } else {
                        try {
                            bot.execute(botUtils.plainMessage(
                                    "Didn't save new label: it's name's length must be less than " +
                                            DataUtils.LABEL_NAME_LENGTH +
                                            " symbols and it can't contain line breaks",
                                    botUtils.retrieveChat(request.update).getId()
                            ));
                        } catch (TelegramApiException e) {
                            logger.error(
                                    String.format("Couldn't inform %s about wrong label name in request %s",
                                            client, request),
                                    e);
                        }
                    }
                }
            } else {
                if (!processOther(DataSaver.class, request)) {
                    setStage(stageDataSaved);
                }
            }
        }
        return false;
    }

    private void init(Request request) {
        SendMessage sendLabelInfo = botUtils.markdownMessage(
                uiUtils.getLabelNavigationInfo(),
                botUtils.retrieveChat(request.update).getId());

        sendLabelInfo.setReplyMarkup(botUtils.oneColumnReplyMarkup(Arrays.asList(
                "Send data by selected label(s)",
                "Rename label",
                "Delete label",
                "To main menu"
        )));

        try {
            bot.execute(sendLabelInfo);

            updatePath("all data");
            updateNavigator(labelUtils.obtain("all data", getClient().getId()), request);
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize LabelNavigator and send dat aof 'all data' in request " + request, e);
        }
    }

    private void updateNavigator(Label label, Request request) {
        // if gonna move to this label
        if (getState().has(label.getName()) || labelUtils.isLabelInPath(label.getName(), getPath())) {
            updatePath(label.getName()); // move
            if (getState().has("CALLBACK MESSAGE")) {
                DeleteMessage deleteMessage = new DeleteMessage()
                        .setMessageId(Integer.parseInt(getState().get("CALLBACK MESSAGE").toString()))
                        .setChatId(Long.parseLong(getState().get("CALLBACK CHAT").toString()));
                try {
                    bot.execute(deleteMessage);
                } catch (TelegramApiException e) {
                    logger.error("Couldn't delete old callback message in request " + request, e);
                }
            }

            if (getState().has(label.getName())) {
                getState().remove(label.getName());
                setState(getState());
            }

            SendMessage sendLabelContents = botUtils.plainMessage(
                    labelUtils.createDataList(label), botUtils.retrieveChat(request.update).getId());
            sendLabelContents.setReplyMarkup(createEmojiMarkup(label));

            try {
                Message callbackMessage = bot.execute(sendLabelContents);
                setState(getState().put("CALLBACK MESSAGE", callbackMessage.getMessageId()));
                setState(getState().put("CALLBACK CHAT", callbackMessage.getChatId()));
            } catch (TelegramApiException e) {
                logger.error("Failed to check sublabels of label info in request " + request, e);
            }
        } else { // just selected a label
            Label parent = labelRepository.findByNameAndClientId(
                    labelUtils.getCurrentDir(getPath()), getClient().getId()
            ).orElseThrow(() -> new IllegalStateException("Label '"
                    + labelUtils.getCurrentDir(getPath()) + "' does not exist"));

            setState(getState().put(label.getName(), "selected"));
            try {
                bot.execute(new EditMessageReplyMarkup()
                        .setMessageId(Integer.parseInt(getState().get("CALLBACK MESSAGE").toString()))
                        .setChatId(Long.parseLong(getState().get("CALLBACK CHAT").toString()))
                        .setReplyMarkup(createEmojiMarkup(parent)));
            } catch (TelegramApiException e) {
                logger.error("Failed to check label info in request " + request, e);
            }
        }
    }

    private InlineKeyboardMarkup createEmojiMarkup(Label label) {
        InlineKeyboardMarkup sublabelsMarkup = labelUtils.createNavigationMarkup(label, getPath());
        sublabelsMarkup.getKeyboard().forEach(inlineKeyboardButtons ->
                inlineKeyboardButtons.forEach(button -> {
                    String emoji;
                    if (getState().has(button.getText()))
                        emoji = EMOJI_CHECK;
                    else if (button.getText().matches(".*[A-Z].*")) {
                        emoji = EMOJI_LEFT;
                    } else {
                        emoji = EMOJI_CROSS;
                    }
                    button.setText(emoji + button.getText());
                }));
        return sublabelsMarkup;
    }

    private void assignLabelToData(String label, String dataUrl, Request request) {
        request.put(LabelAssigner.Key.URL.name(), dataUrl);
        request.put(LabelAssigner.Key.LABEL_NAME.name(), label);
        processStateless(LabelAssigner.class, request);
    }

    private void updatePath(String labelName) {
        setPath(labelUtils.updatePath(labelName, getPath()));
    }

    private String getPath() {
        if (!getState().has("PATH"))
            setState(getState().put("PATH", ""));
        return getState().get("PATH").toString();
    }

    private void setPath(String path) {
        setState(getState().put("PATH", path));
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

    @Autowired
    public void setLabelUtils(LabelUtils labelUtils) {
        this.labelUtils = labelUtils;
    }

    private final Logger logger = LoggerFactory.getLogger(LabelNavigator.class);
}