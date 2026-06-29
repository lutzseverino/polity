// Pre-rendered seal art. A clean SVG seal (concentric rings, a ring of small
// stars, and a jewelled eight-point star) was rasterised through a headless
// browser and sampled into a luminance ramp offline, then pasted here. To change
// the seal, re-run that conversion on a new source image/SVG and replace these
// rows. Rows are padded to SEAL_WIDTH at render; colour is assigned by radius.

export const SEAL_WIDTH = 53;

export const SEAL_LINES: readonly string[] = [
  "                    .+%@@@@@@@%+.",
  "               .%@=    -+***+-    =@%.",
  "            :@=  +=               =+  =@:",
  "          ##  #           *           #  ##",
  "        ** =.     .      .:       .     .= **",
  "      .@ -.       --             --       .- @.",
  "     *- +                                   + -*",
  "    %..     @.      %=         =%      .@     ..%",
  "   %.:           #.       @       .#           :.%",
  "  +-           *.         @         .*           -+",
  "  % =   =     %  :=      *@*      --  %     =   = %",
  " % +   .+    #    .@@.   @@@   .@@.    #    +.   + %",
  " @ -        %      .@@@%:@@@:#@@@.      %        - @",
  "--          :        @@@@@@@@@@@        :          --",
  "* -        +        -*@@#   #@@*-        +        - *",
  "* *   %*   * -@@@@@@@@@@ %@% @@@@@@@@@@: *   *%   * *",
  "* -        +        -*@@#   #@@*-        +        - *",
  "--          :        @@@@@@@@@@@        :          --",
  " @ -        %      .@@@#:@@@:%@@@.      %        - @",
  " % +   .#.   #    .@@.   @@@   .@@.    #   .#.   + %",
  "  % =  :      %  --      *@*      =:  %      :  = %",
  "  +-           *.         @         .*           -+",
  "   %.:           #.       @       .#           :.%",
  "    %..    -@*      %=         =%      *@-    ..%",
  "     *- +                                   + -*",
  "      .@ -.                               .- @.",
  "        ** =.     ##      #      ##     .= **",
  "          ##  #          . .          #  ##",
  "            :@=  +=               =+  =@:",
  "               .%@=    -+***+-    =@%.",
  "                    .+%@@@@@@@%+.",
];
