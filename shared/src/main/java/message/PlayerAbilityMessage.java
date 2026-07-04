package message;

import lombok.Data;
import message.dto.AbilitySlot;

@Data
public class PlayerAbilityMessage {
    private AbilitySlot abilitySlot;
    private int targetX;
    private int targetY;
}
