/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.malp.application.adapters;

import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseHandler;

public interface LibraryItem {

    /**
     * Gets item main text
     * @return item main text or null
     */
    String getMainText();

    /**
     * Gets item prefix text
     * @return item prefix text or null
     */
    String getPrefixText();

    /**
     * Gets item postfix text
     * @return item postfix text or null
     */
    String getPostfixText();

    /**
     * @param handler response handler to handle the result of the MPD query
     * @param listPosition item position in the library RecyclerView
     */
    void getKidItems(MPDResponseHandler handler, int listPosition);

    /**
     * Gets item level, i.e. how many ascendants it has
     * @return item level
     */
    int getLevel();

    /**
     * Checks if the children of this item are expanded
     * @return true if expanded, false otherwise
     */
    boolean isExpanded();

    /**
     * Sets state whether children are expanded
     * @param expanded true to expand, false otherwise
     */
    void setExpanded(boolean expanded);

    /**
     * Gets the type of item in the list this is
     * @return integer signifying Artist, Album or Track item
     */
    int getViewType();

    /**
     * Sets the view holder for this list item
     * @param holder new ViewHolder for that item
     */
    void setViewHolder(LibraryAdapter.ViewHolder holder);

    /**
     * Gets the view holder for this list item
     * @return ViewHolder of the item
     */
    LibraryAdapter.ViewHolder getViewHolder();
}
