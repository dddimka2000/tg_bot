package org.example.service;

import lombok.extern.log4j.Log4j2;
import org.example.config.BotConfig;
import org.example.entity.ClientEntity;
import org.example.entity.Company;
import org.example.entity.UserEntity;
import org.example.repository.ClientEntityRepository;
import org.example.repository.CompanyRepository;
import org.example.repository.UserRepo;
import org.example.validator.UserDataValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@Log4j2
public class TelegramBot extends TelegramLongPollingBot {
    private Boolean register = false;
    private Boolean editPersonalData = false;
    private Boolean addClientBoolean = false;
    private Integer stepsRegister = 0;
    private UserEntity userDto = new UserEntity();
    private ClientEntity clientEntity = new ClientEntity();
    final
    UserRepo userRepo;
    private final BotConfig botConfig;
    final
    ClientEntityRepository clientEntityRepository;
    final
    UserDataValidator userDataValidator;
    final
    CompanyRepository companyRepository;

    public TelegramBot(BotConfig botConfig, UserRepo userRepo, UserDataValidator userDataValidator, ClientEntityRepository clientEntityRepository, CompanyRepository companyRepository) throws TelegramApiException {
        this.botConfig = botConfig;
        execute(new DeleteMyCommands());
        this.userRepo = userRepo;
        this.userDataValidator = userDataValidator;
        this.clientEntityRepository = clientEntityRepository;
        this.companyRepository = companyRepository;
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().toString().trim().length() > 0) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (register == true) {
                registerStart(chatId, message);
            } else if (editPersonalData == true) {
                editStart(chatId, message);
            } else if (addClientBoolean == true) {
                addClientStart(chatId, message);
            } else {
                Boolean step=false;
                switch (message) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "Регистрация":
                        if (userRepo.findById(chatId).isEmpty()) {
                            register = true;
                            step=true;
                            sendMessage(chatId, "Введите ваш номер телефона");
                        } else {
                            register = false;
                            step=true;
                            sendMessage(chatId, "Уже зарегистрированы");
                        }
                        break;
                }
                if (userRepo.findById(chatId).isPresent()) {
                    switch (message) {
                        case "Добавить пользователя":
                            addClientBoolean = true;
                            sendMessage(chatId, "Введите ваш номер телефона");
                            break;
                        case "Информация о себе":
                            sendMessage(chatId, userRepo.findById(chatId).get().toString());
                            break;
                        case "Редактировать информацию о себе":
                            editPersonalData = true;
                            sendMessage(chatId, "Введите ваш номер телефона");
                            break;
                        case "Присоединиться к закрытому каналу":
                            String link="https://uk.wikipedia.org/wiki/Ку-клукс-клан";
                            sendMessage(chatId, "Ссылка на наше сообщество: "+link);
                            break;
                        default:
                            sendMessage(chatId, "Выберите команду с панели кнопок");
                    }
                } else {
                    if(step!=true)
                    sendMessage(chatId, "Для начала зарегистрируйтесь");
                }
            }
        }

    }

    private void addClientStart(Long chatId, String message) {
        if (message.equals("Отменить и вернуться в главное меню")) {
            addClientBoolean = false;
            clientEntity.setNameCompany(null);
            stepsRegister = 0;
            sendMessage(chatId, "Действие отменено");
        } else if (stepsRegister == 0) {
            if (userDataValidator.isValidPhoneNumber(message)) {
                clientEntity.setPhone(message);
                sendMessage(chatId, "Введите ваше имя и фамилию");
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректный номер телефона. Пример номера: +380962221133");
            }
        } else if (stepsRegister == 1) {
            if (userDataValidator.isValidNameAndSurname(message) && message.length() < 80) {
                sendMessage(chatId, "Введите ваш E-mail");
                clientEntity.setNameAndSurname(message);
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректное имя и фамилия. Пример: \"Виктор Викторович\"");
            }
        } else if (stepsRegister == 2) {
            if (userDataValidator.isValidEmail(message) && message.length() < 80) {
                if (clientEntityRepository.findByEmail(message).isEmpty()) {
                    clientEntity.setEmail(message);
                    stepsRegister++;
                    sendMessage(chatId, "Укажите компанию");
                } else {
                    sendMessage(chatId, "Данный E-mail уже занят");
                }
            } else {
                sendMessage(chatId, "Некорректный E-mail. Пример: \"example@example.com\"");
            }
        } else if (stepsRegister == 3) {
            if (companyRepository.findByName(message).isPresent()) {
                clientEntity.setNameCompany(message);
                stepsRegister++;
                log.info(message);
                sendMessage(chatId, "Укажите дату в формате YYYY-MM-DD");
            } else {
                sendMessage(chatId, "Такой компании не существует");
            }
        } else if (stepsRegister == 4) {
            try {
                LocalDate localDate = LocalDate.parse(message);
                clientEntity.setLocalDate(localDate);
                stepsRegister = 0;
                addClientBoolean = false;
                clientEntityRepository.save(clientEntity);
                sendMessage(chatId, "Регистрация клиента успешно завершена завершена.\n"+clientEntity);
            } catch (DateTimeParseException e){
                sendMessage(chatId, "Отправьте в таком формате YYYY-MM-DD. Пример :\"2023-12-02\"");
            }

        }
    }


    private void startCommandReceived(long id, String name) {
        String answer = "Hi, " + name;
        sendMessage(id, answer);
    }

    private void sendMessage(long id, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(id);
        message.setText(text);
        addButtons(id, message);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e);
        }
    }

    private void addButtons(Long id, SendMessage message) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> list = new ArrayList<>();
        if (editPersonalData == true || register == true) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add("Отменить и вернуться в главное меню");
            list.add(keyboardRow);
        } else if (addClientBoolean == true) {

            if (stepsRegister == 3) {
                List<Company> all = companyRepository.findAll();
                log.info(all);
                if (all.size() > 0) {
                    log.info(all.size());
                    for (int i = 0; i < all.size()-1; i = i + 3) {
                        log.info(i+ "  "+all.size());
                        KeyboardRow keyboardRowNew = new KeyboardRow();
                        keyboardRowNew.add(all.get(i).getName());
                        log.info(all.get(i).getName());
                        if ((i + 1) < all.size()) {
                            log.info("add+1");
                            keyboardRowNew.add(all.get(i + 1).getName());
                        }
                        if (i + 2 < all.size()) {
                            log.info("add+2");
                            keyboardRowNew.add(all.get(i + 2).getName());
                        }
                        list.add(keyboardRowNew);
                    }
                }
            }
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add("Отменить и вернуться в главное меню");
            list.add(keyboardRow);
        } else {
            if (userRepo.findById(id).isPresent()) {
                KeyboardRow keyboardRow2 = new KeyboardRow();
                keyboardRow2.add("Добавить пользователя");
                keyboardRow2.add("Информация о себе");
                KeyboardRow keyboardRow = new KeyboardRow();
                keyboardRow.add("Редактировать информацию о себе");
                list.add(keyboardRow);
                list.add(keyboardRow2);
            } else {
                KeyboardRow keyboardRow = new KeyboardRow();
                keyboardRow.add("/start");
                keyboardRow.add("Регистрация");
                list.add(keyboardRow);
            }
        }
        replyKeyboardMarkup.setKeyboard(list);
        message.setReplyMarkup(replyKeyboardMarkup);
    }

    private void registerStart(Long chatId, String message) {
        if (message.equals("Отменить и вернуться в главное меню")) {
            register = false;
            userDto.setNameCompany(null);
            stepsRegister = 0;
            sendMessage(chatId, "Действие отменено");
        } else if (stepsRegister == 0) {
            if (userDataValidator.isValidPhoneNumber(message)) {
                userDto.setPhone(message);
                sendMessage(chatId, "Введите ваше имя и фамилию");
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректный номер телефона. Пример номера: +380962221133");
            }
        } else if (stepsRegister == 1) {
            if (userDataValidator.isValidNameAndSurname(message) && message.length() < 80) {
                sendMessage(chatId, "Введите ваш E-mail");
                userDto.setNameAndSurname(message);
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректное имя и фамилия. Пример: \"Виктор Викторович\"");
            }
        } else if (stepsRegister == 2) {
            if (userDataValidator.isValidEmail(message) && message.length() < 80) {
                if (userRepo.findByEmail(message).isEmpty()) {
                    sendMessage(chatId, "Являетесь ли вы сотрудником агентства? (Да/Нет)");
                    userDto.setEmail(message);
                    stepsRegister++;
                } else {
                    sendMessage(chatId, "Данный E-mail уже занят");
                }
            } else {
                sendMessage(chatId, "Некорректный E-mail. Пример: \"example@example.com\"");
            }
        } else if (stepsRegister == 3) {
            switch (message.substring(0, 1).toUpperCase() + message.substring(1).toLowerCase()) {
                case "Да":
                    sendMessage(chatId, "Укажите компанию");
                    stepsRegister++;
                    break;
                case "Нет":
                    register = false;
                    userDto.setId(chatId);
                    userRepo.save(userDto);
                    stepsRegister = 0;
                    sendMessage(chatId, "Регистрация успешно завершена");
                    break;
                default:
                    sendMessage(chatId, "Укажите \"Да\" или \"Нет\"");
            }
        } else if (stepsRegister == 4) {
            if (message.length() < 100) {
                userDto.setNameCompany(message);
                register = false;
                stepsRegister = 0;
                userDto.setId(chatId);
                userRepo.save(userDto);

                sendMessage(chatId, "Регистрация успешно завершена");
            } else {
                sendMessage(chatId, "Длина превышает 100 символов");
            }

        }
    }

    private void editStart(Long chatId, String message) {
        if (message.equals("Отменить и вернуться в главное меню")) {
            editPersonalData = false;
            userDto.setNameCompany(null);
            stepsRegister = 0;
            sendMessage(chatId, "Действие отменено");
        } else if (stepsRegister == 0) {
            if (userDataValidator.isValidPhoneNumber(message)) {
                userDto.setPhone(message);
                sendMessage(chatId, "Введите ваше имя и фамилию");
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректный номер телефона. Пример номера: +380962221133");
            }
        } else if (stepsRegister == 1) {
            if (userDataValidator.isValidNameAndSurname(message) && message.length() < 80) {
                sendMessage(chatId, "Введите ваш E-mail");
                userDto.setNameAndSurname(message);
                stepsRegister++;
            } else {
                sendMessage(chatId, "Некорректное имя и фамилия. Пример: \"Виктор Викторович\"");
            }
        } else if (stepsRegister == 2) {
            if (userDataValidator.isValidEmail(message) && message.length() < 80) {
                if (userRepo.findByEmail(message).isEmpty() || userRepo.findById(chatId).get().getEmail().equals(message)) {
                    sendMessage(chatId, "Являетесь ли вы сотрудником агентства? (Да/Нет)");
                    userDto.setEmail(message);
                    stepsRegister++;
                } else {
                    sendMessage(chatId, "Данный E-mail уже занят");
                }
            } else {
                sendMessage(chatId, "Некорректный E-mail. Пример: \"example@example.com\"");
            }
        } else if (stepsRegister == 3) {
            switch (message.substring(0, 1).toUpperCase() + message.substring(1).toLowerCase()) {
                case "Да":
                    sendMessage(chatId, "Укажите компанию");
                    stepsRegister++;
                    break;
                case "Нет":
                    editPersonalData = false;
                    userDto.setId(chatId);
                    userRepo.save(userDto);
                    stepsRegister = 0;
                    sendMessage(chatId, "Профиль успешно отредактирован");
                    break;
                default:
                    sendMessage(chatId, "Укажите \"Да\" или \"Нет\"");
            }
        } else if (stepsRegister == 4) {
            if (message.length() < 100) {
                userDto.setNameCompany(message);
                editPersonalData = false;
                stepsRegister = 0;
                userDto.setId(chatId);
                userRepo.save(userDto);
                sendMessage(chatId, "Профиль успешно отредактирован");
            } else {
                sendMessage(chatId, "Длина превышает 100 символов");
            }

        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();

    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

}
