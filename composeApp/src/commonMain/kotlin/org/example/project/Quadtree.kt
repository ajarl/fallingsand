package org.example.project

interface TwoDPoint {
    val x: Int
    val y: Int
}

sealed class TreeNode<T : TwoDPoint> {
    data class Node<T : TwoDPoint>(
        val splitX: Int,
        val splitY: Int,
        val topLeft: TreeNode<T>,
        val topRight: TreeNode<T>,
        val bottomLeft: TreeNode<T>,
        val bottomRight: TreeNode<T>,
    ) : TreeNode<T>()

    data class Leaf<T : TwoDPoint>(
        val points: MutableList<T>
    ) : TreeNode<T>()
}

const val DEPTH = 8

class Quadtree<T : TwoDPoint>(width: Int, height: Int) {
    val activePoints = mutableListOf<T>()
    val restedPoints = mutableListOf<T>()

    private var root: TreeNode.Node<T> = run {
        fun createLayer(
            level: Int,
            splitX: Int,
            splitY: Int,
        ): TreeNode.Node<T> {
            if (level == DEPTH - 1) {
                return TreeNode.Node(
                    splitX = splitX,
                    splitY = splitY,
                    topLeft = TreeNode.Leaf<T>(mutableListOf()),
                    topRight = TreeNode.Leaf<T>(mutableListOf()),
                    bottomLeft = TreeNode.Leaf<T>(mutableListOf()),
                    bottomRight = TreeNode.Leaf<T>(mutableListOf()),
                )
            }

            val layerWidth = width shr level
            val layerHeight = height shr level

            return TreeNode.Node(
                splitX = splitX,
                splitY = splitY,
                topLeft = createLayer(level + 1, splitX - layerWidth / 2, splitY - layerHeight / 2),
                topRight = createLayer(level + 1, splitX + layerWidth / 2, splitY - layerHeight / 2),
                bottomLeft = createLayer(level + 1, splitX - layerWidth / 2, splitY + layerHeight / 2),
                bottomRight = createLayer(level + 1, splitX + layerWidth / 2, splitY + layerHeight / 2),
            )
        }

        createLayer(1, width / 2, height / 2)
    }

    private fun findLeafNode(x: Int, y: Int): TreeNode.Leaf<T> {
        var node: TreeNode<T> = root
        while (node is TreeNode.Node<T>) {
            node = when {
                x < node.splitX && y < node.splitY -> node.topLeft
                x >= node.splitX && y < node.splitY -> node.topRight
                x < node.splitX && y >= node.splitY -> node.bottomLeft
                else -> node.bottomRight
            }
        }

        return node as TreeNode.Leaf<T>
    }

    fun insert(point: T) {
        activePoints.add(point)
        findLeafNode(point.x, point.y).points.add(point)
    }

    fun onPositionUpdated(point: T, oldX: Int, oldY: Int) {
        val old = findLeafNode(oldX, oldY)
        val new = findLeafNode(point.x, point.y)
        if (old !== new) {
            old.points.remove(point)
            new.points.add(point)
        }
    }

    fun lookup(x: Int, y: Int): T? {
        val node = findLeafNode(x, y)

        return node.points.find { it.x == x && it.y == y}
    }
}
