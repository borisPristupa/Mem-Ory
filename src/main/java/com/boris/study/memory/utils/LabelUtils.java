package com.boris.study.memory.utils;

import com.boris.study.memory.data.entity.Data;
import com.boris.study.memory.data.entity.Label;
import com.boris.study.memory.data.repository.DataRepository;
import com.boris.study.memory.data.repository.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LabelUtils {
    public static final String DELIMITER = "I";

    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private DataRepository dataRepository;

    public Label obtain(String name, Long clientId) throws IllegalStateException {
        return labelRepository.findByNameAndClientId(name, clientId)
                .orElseThrow(() -> new IllegalStateException("No label '" + name + "'"));
    }

    // Only works correctly if you update your path to this label first
    public InlineKeyboardMarkup createNavigationMarkup(Label label, String path) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        if (null != label.getParents() && !label.getParents().isEmpty()) {
            InlineKeyboardButton levelUp = new InlineKeyboardButton("Go back to '" +
                    getLevelUpDir(path) + "'");
            levelUp.setCallbackData(getLevelUpDir(path));
            buttons.add(Collections.singletonList(levelUp));
        }

        int labelsInRow = label.getSons().size() > 4 ? 3 : 2;
        List<Label> sons = new ArrayList<>(label.getSons());
        sons.sort(Comparator.comparing(Label::getName));

        for (int i = 0; i < sons.size() / labelsInRow + 1; i++) {
            List<InlineKeyboardButton> row = new LinkedList<>();
            for (int j = 0; j < labelsInRow && i * labelsInRow + j < sons.size(); j++) {
                Label son = sons.get(i * labelsInRow + j);
                InlineKeyboardButton labelButton = new InlineKeyboardButton(son.getName());
                labelButton.setCallbackData(son.getName());
                row.add(labelButton);
            }
            if (!row.isEmpty())
                buttons.add(row);
        }
        return new InlineKeyboardMarkup().setKeyboard(buttons);
    }

    public String createDataList(Label label) {
        StringBuilder dataContent =
                new StringBuilder("Here is a list of data, marked by '" + label.getName() + "'\n")
                        .append("-------------------------------------");
        int lengthWithoutData = dataContent.length();

        Set<String> labelNamesRecursive = label.getAllSonsRecursively().stream()
                .map(Label::getName)
                .collect(Collectors.toSet());

        Set<Data> labeledData =
                dataRepository.findAllByLabelNamesAndClientId(labelNamesRecursive, label.getClient().getId());

        int i = 1;
        for (Iterator<Data> it = labeledData.iterator(); it.hasNext(); i++) {
            Data data = it.next();
            dataContent.append("\n")
                    .append(i).append(". ") // number of data in the list
                    .append(data.getUrl()) // data's url
                    .append(" - ").append(data.getDescription()).append("\n"); // data's description
        }

        if (dataContent.length() == lengthWithoutData) {
            dataContent.append("\n").append("Well, no data yet");
        }

        return dataContent.toString();
    }

    public String updatePath(String labelName, String path) {
        if (isLabelInPath(labelName, path)) {
            return path.substring(0, path.indexOf(labelName) + labelName.length());
        } else {
            return path + DELIMITER + labelName;
        }
    }

    public boolean isLabelInPath(String label, String path) {
        return Arrays.asList(path.split(DELIMITER)).contains(label);
    }

    public String getCurrentDir(String path) {
        String[] hierarchy = path.split(DELIMITER);
        if (hierarchy.length > 0) {
            return hierarchy[hierarchy.length - 1];
        } else {
            return "NONE";
        }
    }

    public String getLevelUpDir(String path) {
        String[] hierarchy = path.split(DELIMITER);
        if (hierarchy.length > 1) {
            return hierarchy[hierarchy.length - 2];
        } else {
            return "NONE";
        }
    }
}
