package emu.grasscutter.game.ability.actions;

import com.google.protobuf.ByteString;
import emu.grasscutter.data.binout.AbilityModifier;
import emu.grasscutter.game.ability.Ability;
import emu.grasscutter.game.entity.EntityAvatar;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.net.proto.PropChangeReasonOuterClass;
import emu.grasscutter.net.proto.ChangeHpDebtsReasonOuterClass.ChangeHpDebtsReason;
import emu.grasscutter.server.packet.send.PacketEntityFightPropChangeReasonNotify;
import emu.grasscutter.server.packet.send.PacketEntityFightPropUpdateNotify;
import emu.grasscutter.Grasscutter;

@AbilityAction(value = AbilityModifier.AbilityModifierAction.Type.ReduceHPDebts)
public final class ActionReduceHPDebts extends AbilityActionHandler {
    @Override
    public boolean execute(Ability ability, AbilityModifier.AbilityModifierAction action, ByteString abilityData, GameEntity target) {
        // Calculate the amount to reduce based on the action ratio
        float reduction = action.ratio.get(ability);
        Grasscutter.getLogger().warn("[ActionReduceHPDebts] Called with reduction {}", reduction);

        if (target instanceof EntityAvatar) {
            float curDebt = target.getFightProperty(FightProperty.FIGHT_PROP_CUR_HP_DEBTS);
            float newDebt = curDebt - reduction;

            // Ensure new debt doesn't go below zero
            if (newDebt < 0) {
                newDebt = 0;
            }

            float changeDebt = curDebt - newDebt;

            // Update the fight property with the new debt value
            target.setFightProperty(FightProperty.FIGHT_PROP_CUR_HP_DEBTS, newDebt);
            target.getWorld().broadcastPacket(new PacketEntityFightPropUpdateNotify(target, FightProperty.FIGHT_PROP_CUR_HP_DEBTS));

            // Notify about the change reason
            if (changeDebt != 0) {
                if (newDebt == 0) {
                    target.getWorld().broadcastPacket(new PacketEntityFightPropChangeReasonNotify(target, FightProperty.FIGHT_PROP_CUR_HP_DEBTS, changeDebt, PropChangeReasonOuterClass.PropChangeReason.PROP_CHANGE_REASON_ABILITY, ChangeHpDebtsReason.CHANGE_HP_DEBTS_CLEAR));
                } else {
                    target.getWorld().broadcastPacket(new PacketEntityFightPropChangeReasonNotify(target, FightProperty.FIGHT_PROP_CUR_HP_DEBTS, changeDebt, PropChangeReasonOuterClass.PropChangeReason.PROP_CHANGE_REASON_ABILITY, ChangeHpDebtsReason.CHANGE_HP_DEBTS_REDUCE_ABILITY));
                }
            }
        } else {
            Grasscutter.getLogger().warn("[ActionReduceHPDebts] CANNOT REDUCE HP DEBT FOR NON AVATAR ENTITY");
            return false;
        }
        return true;
    }
}
