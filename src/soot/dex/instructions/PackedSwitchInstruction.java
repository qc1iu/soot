/* Soot - a Java Optimization Framework
 * Copyright (C) 2012 Michael Markert, Frank Hartmann
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.dex.instructions;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;

import soot.Local;
import soot.Unit;
import soot.dex.DexBody;
import soot.jimple.Jimple;
import soot.jimple.Stmt;

public class PackedSwitchInstruction extends SwitchInstruction {

    public PackedSwitchInstruction (Instruction instruction, int codeAdress) {
        super(instruction, codeAdress);
    }

    protected Stmt switchStatement(DexBody body, Instruction targetData, Local key) {
        PackedSwitchDataPseudoInstruction i = (PackedSwitchDataPseudoInstruction) targetData;
        int[] targetAddresses = i.getTargets();
        int lowIndex = i.getFirstKey();
        int highIndex = lowIndex + targetAddresses.length - 1;
        // the default target always follows the switch statement
        int defaultTargetAddress = codeAddress + instruction.getSize(codeAddress);
        Unit defaultTarget = body.instructionAtAddress(defaultTargetAddress).getUnit();
        List<Unit> targets = new ArrayList<Unit>();
        for(int address : targetAddresses)
            targets.add(body.instructionAtAddress(codeAddress + address).getUnit());

        return Jimple.v().newTableSwitchStmt(key, lowIndex, highIndex, targets, defaultTarget);
    }

    @Override
    public void computeDataOffsets(DexBody body) {
      int offset = ((OffsetInstruction) instruction).getTargetAddressOffset();
      int targetAddress = codeAddress + offset;
      Instruction targetData = body.instructionAtAddress(targetAddress).instruction;
      PackedSwitchDataPseudoInstruction psInst = (PackedSwitchDataPseudoInstruction) targetData;
      int[] targetAddresses = psInst.getTargets();
      int size = targetAddresses.length;
      
      // From org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction we learn
      // that there are 2 bytes after the magic number that we have to jump.
      // 2 bytes to jump = address + 1
      //
      //      out.writeByte(0x00); // magic
      //      out.writeByte(0x01); // number
      //      out.writeShort(targets.length); // 2 bytes
      //      out.writeInt(firstKey);
      
      setDataFirstByte (targetAddress + 1);
      setDataLastByte (targetAddress + 1 + size - 1);
      setDataSize (size);
      
      ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
      psInst.write(out, targetAddress);

      // include all bytes as data since dexlib needs this
      byte[] outa = out.getArray();
      setData (outa);
      
    }
}
